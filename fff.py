"""fff.py"""

import sys

### Lexer

class Origin(object):

  def __init__(self, filespec, string, position):
    self.filespec = filespec
    self.string = string
    self.position = position

  def LocationMessage(self):
    return 'in %s on line %d column %d\n%s\n%s*\n' % (
        self.filespec,
        self.LineNumber(),
        self.ColumnNumber(),
        self.Line(),
        ' ' * (self.ColumnNumber() - 1))

  def LineNumber(self):
    return 1 + self.string.count('\n', 0, self.position)

  def ColumnNumber(self):
    return 1 + self.position - self.LineStart()

  def Line(self):
    return self.string[self.LineStart():self.LineEnd()]

  def LineStart(self):
    return self.string.rfind('\n', 0, self.position) + 1

  def LineEnd(self):
    p = self.string.find('\n', self.position)
    return len(self.string) if p == -1 else p


class Token(object):

  def __init__(self, origin, type, value):
    self.origin = origin
    self.type = type
    self.value = value

  def __eq__(self, other):
    return self.type == other.type and self.value == other.value

  def __repr__(self):
    return 'Token(%r,%r)' % (self.type, self.value)


class CclError(Exception):

  def __init__(self, message, origin):
    super(CclError, self).__init__(message + '\n' + origin.LocationMessage())


class LexError(CclError):
  pass


class Lexer(object):

  def __init__(self, filespec, string):
    self.filespec = filespec
    self.s = string
    self.i = 0
    self.j = 0
    self.indent_stack = ['']
    self.depth = 0
    self.tokens = []

  def GetSymbols(self):
    return [
      '.', '=', '(', ')', '[', ']', ',', ';', ':',
    ]

  def GetKeywords(self):
    return [
      'class',
      'pass',
      'method',
      'var',
      'return',
      'while', 'break', 'continue',
      'if', 'else',
    ]

  def MakeOrigin(self):
    return Origin(self.filespec, self.s, self.j)

  def MakeToken(self, type_, value):
    return Token(self.MakeOrigin(), type_, value)

  def Char(self):
    return self.s[self.i]

  def Done(self):
    return self.i >= len(self.s)

  def NotDone(self):
    return self.i < len(self.s)

  def SkipSpacesAndComments(self):
    "skips newlines iff depth > 0"
    while self.NotDone() and ((self.Char().isspace() and (self.depth or self.Char() != '\n')) or self.Char() == '#'):
      if self.Char() == '#':
        while self.NotDone() and self.Char() != '\n':
          self.i += 1
      else:
        self.i += 1

  def ProcessIndents(self):
    indent = self.s[self.j:self.i]
    if indent == self.indent_stack[-1]:
      pass
    elif indent.startswith(self.indent_stack[-1]):
      self.tokens.append(self.MakeToken('Indent', None))
      self.tokens.append(self.MakeToken('Newline', None))
      self.indent_stack.append(indent)
    elif indent in self.indent_stack:
      while indent != self.indent_stack[-1]:
        self.tokens.append(self.MakeToken('Dedent', None))
        self.tokens.append(self.MakeToken('Newline', None))
        self.indent_stack.pop()
    else:
      raise LexError('Invalid indent: ' + repr(indent), self.MakeOrigin())

  def Slice(self):
    return self.s[self.j:self.i]

  def Lex(self):
    while True:
      self.SkipSpacesAndComments()

      self.j = self.i

      if self.Done():
        break

      elif self.Char() == '\n':
        self.i += 1
        self.tokens.append(self.MakeToken('Newline', None))
        while True:
          self.j = self.i
          while self.NotDone() and self.Char().isspace() and self.Char() != '\n':
            self.i += 1
          if self.NotDone() and self.Char() == '#':
            while self.NotDone() and self.Char() != '\n':
              self.i += 1
          if self.Done() or not self.Char().isspace():
            break
          self.i += 1
        if self.NotDone():
          self.ProcessIndents()

      elif self.Char().isdigit() or self.Char() == '.' and self.s[self.i+1:self.i+2].isdigit():
        while self.NotDone() and self.Char().isdigit():
          self.i += 1
        if self.NotDone() and self.Char() == '.':
          self.i += 1
          while self.NotDone() and self.Char().isdigit():
            self.i += 1
        self.tokens.append(self.MakeToken('Number', eval(self.Slice())))

      elif self.s.startswith(('r"', "r'", '"', "'"), self.i):
        raw = False
        if self.Char() == 'r':
          self.i += 1
          raw = True
        quote = self.s[self.i:self.i+3] if self.s.startswith(('"""', "'''"), self.i) else self.s[self.i:self.i+1]
        self.i += len(quote)
        while not self.s.startswith(quote, self.i):
          if self.Done():
            raise LexError("Missing quotes for: " + quote, self.MakeOrigin())
          self.i += 2 if not raw and self.s[self.i] == '\\' else 1
        self.i += len(quote)
        self.tokens.append(self.MakeToken('String', eval(self.Slice())))

      elif self.Char().isalnum() or self.Char() == '_':
        while self.NotDone() and (self.Char().isalnum() or self.Char() == '_'):
          self.i += 1
        word = self.Slice()
        if word in self.GetKeywords():
          self.tokens.append(self.MakeToken(word, None))
        else:
          self.tokens.append(self.MakeToken('Name', word))

      elif self.s.startswith(tuple(self.GetSymbols()), self.i):
        symbol = max(s for s in self.GetSymbols() if self.s.startswith(s, self.i))
        if symbol in ('(', '{', '['):
          self.depth += 1
        elif symbol in (')', '}', ']'):
          self.depth -= 1
        self.i += len(symbol)
        self.tokens.append(self.MakeToken(symbol, None))

      else:
        while self.NotDone() and not self.Char().isspace():
          self.i += 1
        raise LexError("Unrecognized token: " + self.Slice(), self.MakeOrigin())

    while self.indent_stack[-1] != '':
      self.tokens.append(self.MakeToken('Dedent', None))
      self.indent_stack.pop()

    self.tokens.append(self.MakeToken('End', None))

    return self.tokens

### Lexer Tests

origin = Origin('<test>', """
hello world!
""", 1)

assert origin.Line() == 'hello world!', repr(origin.Line())

assert origin.LocationMessage() == """in <test> on line 2 column 1
hello world!
*
""", repr(origin.LocationMessage())

tokens = Lexer('<test>', """
"hello".Print()
""").Lex()

assert (
    tokens ==
    [
        Token(None, 'Newline', None),
        Token(None, 'String', 'hello'),
        Token(None, '.', None),
        Token(None, 'Name', 'Print'),
        Token(None, '(', None),
        Token(None, ')', None),
        Token(None, 'Newline', None),
        Token(None, 'End', None),
    ]
), tokens

tokens = Lexer('<test>', """
i = 0
while i.LessThan(10)
  i.Print()
  i = i.Add(1)
""").Lex()

assert (
    tokens ==
    [
        Token(None, 'Newline', None),

        Token(None, 'Name', 'i'),
        Token(None, '=', None),
        Token(None, 'Number', 0),
        Token(None, 'Newline', None),

        Token(None, 'while', None),
        Token(None, 'Name', 'i'),
        Token(None, '.', None),
        Token(None, 'Name', 'LessThan'),
        Token(None, '(', None),
        Token(None, 'Number', 10),
        Token(None, ')', None),
        Token(None, 'Newline', None),

        Token(None, 'Indent', None),

          Token(None, 'Newline', None),

          Token(None, 'Name', 'i'),
          Token(None, '.', None),
          Token(None, 'Name', 'Print'),
          Token(None, '(', None),
          Token(None, ')', None),
          Token(None, 'Newline', None),

          Token(None, 'Name', 'i'),
          Token(None, '=', None),
          Token(None, 'Name', 'i'),
          Token(None, '.', None),
          Token(None, 'Name', 'Add'),
          Token(None, '(', None),
          Token(None, 'Number', 1),
          Token(None, ')', None),
          Token(None, 'Newline', None),

        Token(None, 'Dedent', None),

        Token(None, 'End', None),
    ]
), tokens

try:
  Lexer('<test>', '!@#').Lex()
except LexError as e:
  assert str(e) == """Unrecognized token: !@#
in <test> on line 1 column 1
!@#
*
""", str(e)
else:
  assert False, "Lex('!@#', '<test>') should have raised error but didn't"

### Parser

class ParseError(CclError):
  pass


class Parser(object):

  def __init__(self, filespec, string, tree_builder):
    self.tokens = Lexer(filespec, string).Lex()
    self.i = 0
    self.tree_builder = tree_builder

  def Peek(self, lookahead):
    return self.tokens[self.i + lookahead]

  def At(self, type_, origin_pointer):
    if self.Peek(0).type == type_:
      if origin_pointer:
        origin_pointer[0] = self.Peek(0).origin
      return True

  def GetToken(self):
    token = self.tokens[self.i]
    self.i += 1
    return token

  def Consume(self, type_, origin_pointer):
    if self.At(type_, origin_pointer):
      return self.GetToken()

  def Expect(self, type_, origin_pointer):
    if not self.At(type_, origin_pointer):
      raise ParseError('Expected %s but found %s' % (type_, self.Peek(0).type), self.Peek(0).origin)
    return self.GetToken()

  def EatStatementDelimiters(self):
    while self.Consume('Newline', None) or self.Consume(';', None):
      pass

  def ParseModule(self):
    origin = self.Peek(0).origin
    statements = []
    self.EatStatementDelimiters()
    while not self.At('End', None):
      statements.append(self.ParseStatement())
      self.EatStatementDelimiters()
    return self.tree_builder.Module(origin, statements)

  def ParseAttributeDeclaration(self):
    origin = [None]
    self.Expect('var', origin)
    name = self.Expect('Name', None).value
    type_ = None
    if self.Consume(':', None):
      type_ = self.Expect('Name', None).value
    return self.tree_builder.AttributeDeclaration(origin[0], name, type_)

  def ParseDeclaration(self):
    origin = [None]
    self.Expect('var', origin)
    name = self.Expect('Name', None).value
    type_ = None
    if self.Consume(':', None):
      type_ = self.Expect('Name', None).value
    return self.tree_builder.Declaration(origin[0], name, type_)

  def ParseMethod(self):
    origin = [None]
    self.Expect('method', origin)
    name = self.Expect('Name', None).value
    args = []
    self.Expect('(', None)
    while not self.Consume(')', None):
      argname = self.Expect('Name', None).value
      argtype = None
      if self.Consume(':', None):
        argtype = self.Expect('Name', None).value
      args.append((argname, argtype))
      self.Consume(',', None)
    return_type = None
    if self.Consume(':', None):
      return_type = self.Expect('Name', None).value
    self.EatStatementDelimiters()
    body = self.ParseStatementBlock()
    return self.tree_builder.Method(origin[0], name, args, return_type, body)

  def ParseStatementBlock(self):
    origin = [None]
    self.Expect('Indent', origin)
    self.EatStatementDelimiters()
    statements = []
    while not self.Consume('Dedent', None):
      if self.Consume('pass', None):
        pass
      else:
        statements.append(self.ParseStatement())
      self.EatStatementDelimiters()
    return self.tree_builder.StatementBlock(origin[0], statements)

  def ParseStatement(self):
    origin = [None]
    if self.At('var', None):
      return self.ParseDeclaration()
    elif self.Consume('return', origin):
      return self.tree_builder.Return(origin[0], self.ParseExpression())
    elif self.Consume('while', origin):
      test = self.ParseExpression()
      self.EatStatementDelimiters()
      body = self.ParseStatementBlock()
      return self.tree_builder.While(origin[0], test, body)
    elif self.Consume('break', origin):
      return self.tree_builder.Break(origin[0])
    elif self.Consume('continue', origin):
      return self.tree_builder.Continue(origin[0])
    elif self.Consume('if', origin):
      test = self.ParseExpression()
      self.EatStatementDelimiters()
      body = self.ParseStatementBlock()
      other = None
      self.EatStatementDelimiters()
      if self.Consume('else', None):
        self.EatStatementDelimiters()
        other = self.ParseStatementBlock()
      elif self.Peek(-1).type in (';', 'Newline'): # TODO: Find more elegant solution.
        self.i -= 1
      return self.tree_builder.If(origin[0], test, body, other)
    else:
      origin = self.Peek(0).origin
      expression = self.ParseExpression()
      return self.tree_builder.ExpressionStatement(origin, expression)

  def ParseExpression(self):
    return self.ParseTernaryExpression()

  def ParseTernaryExpression(self):
    origin = [None]
    expr = self.ParseOrExpression()
    if self.Consume('if', origin):
      test = self.ParseExpression()
      self.Expect('else', None)
      right = self.ParseTernaryExpression()
      expr = self.tree_builder.TernaryExpression(origin[0], expr, test, right)
    return expr

  def ParseOrExpression(self):
    origin = [None]
    expr = self.ParseAndExpression()
    while self.Consume('or', origin):
      right = self.ParseAndExpression()
      expr = self.tree_builder.OrExpression(origin[0], expr, right)
    return expr

  def ParseAndExpression(self):
    origin = [None]
    expr = self.ParsePrefixExpression()
    while self.Consume('and', origin):
      right = self.ParsePrefixExpression()
      expr = self.tree_builder.AndExpression(origin[0], expr, right)
    return expr

  def ParsePrefixExpression(self):
    origin = [None]
    if self.Consume('not', origin):
      expr = self.ParsePrefixExpression()
      return self.tree_builder.Not(origin[0], expr)
    elif self.Consume('+', origin):
      expr = self.ParsePrefixExpression()
      return self.tree_builder.Positive(origin[0], expr)
    elif self.Consume('-', origin):
      expr = self.ParsePrefixExpression()
      return self.tree_builder.Negative(origin[0], expr)
    return self.ParsePostfixExpression()

  def ParsePostfixExpression(self):
    origin = [None]
    expr = self.ParsePrimaryExpression()
    while True:
      if self.Consume('(', origin):
        args = self.ParseArgumentList()
        self.Expect(')', None)
        expr = self.tree_builder.FunctionCall(origin[0], expr, args)
      elif self.Consume('.', origin):
        attr = self.Expect('Name', None).value
        if self.Consume('(', origin):
          args = self.ParseArgumentList()
          self.Expect(')', None)
          expr = self.tree_builder.MethodCall(origin[0], expr, attr, args)
        elif self.Consume('=', origin):
          value = self.ParseExpression()
          expr = self.tree_builder.SetAttribute(origin[0], expr, attr, value)
        else:
          expr = self.tree_builder.GetAttribute(origin[0], expr, attr)
      else:
        break
    return expr

  def ParseArgumentList(self):
    args = []
    while not any(self.At(delim, None) for delim in (')', ']')):
      args.append(self.ParseExpression())
      self.Consume(',', None)
    return args

  def ParsePrimaryExpression(self):
    origin = [None]
    if self.At('Name', origin):
      name = self.Expect('Name', None).value
      if self.Consume('=', origin):
        value = self.ParseExpression()
        return self.tree_builder.Assignment(origin[0], name, value)
      else:
        return self.tree_builder.Name(origin[0], name)
    elif self.At('Number', origin):
      number = self.Expect('Number', None).value
      return self.tree_builder.Number(origin[0], number)
    elif self.At('String', origin):
      string = self.Expect('String', None).value
      return self.tree_builder.String(origin[0], string)
    elif self.Consume('[', origin):
      args = self.ParseArgumentList()
      self.Expect(']', None)
      return self.tree_builder.List(origin[0], args)
    elif self.Consume('(', None):
      expr = self.ParseExpression()
      self.Expect(')', None)
      return expr
    raise ParseError('Expected expression', self.Peek(0).origin)


class TreeBuilder(object):

  class Node(object):
    def __init__(self, origin, *args):
      self.origin = origin
      if len(self.attrs) != len(args):
        raise TypeError('When building %s, found %d arguments but expected %d (%s)' % (type(self), len(args), len(self.attrs), ', '.join(self.attrs)))

  class Module(Node):
    attrs = ['statements']

  class Name(Node):
    attrs = ['name']

  class String(Node):
    attrs = ['value']

  class ExpressionStatement(Node):
    attrs = ['expressions']

  class FunctionCall(Node):
    attrs = ['function', 'arguments']

Parser('<filespec>', r'''

include('hi')




''', TreeBuilder).ParseModule()
