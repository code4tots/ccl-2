"""again.py"""

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
      'include',
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
    origin_pointer = [None]
    clss = []
    includes = []
    origin = self.Peek(0).origin
    self.EatStatementDelimiters()
    while not self.At('End', None):
      if self.Consume('include', origin_pointer):
        includes.append(self.tree_builder.Include(origin_pointer[0], self.Expect('String', None).value))
      elif self.At('class', None):
        clss.append(self.ParseClass())
      else:
        raise ParseError('Expected include directive or class definition', self.Peek(0).origin)
      self.EatStatementDelimiters()
    return self.tree_builder.Module(origin, includes, clss)

  def ParseClass(self):
    origin = [None]
    self.Expect('class', origin)
    name = self.Expect('Name', None).value
    bases = []
    declarations = []
    methods = []
    self.Consume(':', None)
    while self.At('Name', None):
      bases.append(self.Expect('Name', None).value)
      self.Consume(',', None)
    self.EatStatementDelimiters()
    self.Expect('Indent', None)
    self.EatStatementDelimiters()
    while not self.Consume('Dedent', None):
      if self.At('var', None):
        declarations.append(self.ParseAttributeDeclaration())
      elif self.At('method', None):
        methods.append(self.ParseMethod())
      elif self.Consume('pass', None):
        pass
      else:
        raise ParseError('Expected declaration or method', self.Peek(0).origin)
      self.EatStatementDelimiters()
    return self.tree_builder.Class(origin[0], name, bases, declarations, methods)

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
      if self.Consume('.', origin):
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
      elif self.Consume('(', origin):
        args = self.ParseArgumentList()
        self.Expect(')', None)
        return self.tree_builder.New(origin[0], name, args)
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

### Parser Tests

class TestTreeBuilder(object):

  def Module(self, origin, includes, classes):
    pass

  def Class(self, origin, name, bases, declarations, methods):
    pass

Parser('<test>', r"""

class Main
  pass

""", TestTreeBuilder()).ParseModule()

### Translator

class TranslationError(CclError):
  pass


class StringWithData(str):
  pass


class Translator(object):

  def __init__(self, filespec, string):
    self.filespec = filespec
    self.string = string
    self.parser = Parser(filespec, string, self)

  def Translate(self):
    return self.GetNativePrelude() + self.parser.ParseModule()

  def GetNativePrelude(self):
    return r"""
import java.util.ArrayList;
import java.util.Arrays;

class CCObject {

  public static CCNil XXnil = new CCNil();

  public CCObject GetAttribute(String name) {
    throw new RuntimeException("Unrecognized attribute " + name);
  }
  public CCObject SetAttribute(String name, CCObject value) {
    throw new RuntimeException("Unrecognized attribute " + name);
  }
  public CCObject InvokeMethod(String name, CCObject... args) {
    throw new RuntimeException("Unrecognized method " + name);
  }
  public boolean ToBoolean() {
    return true;
  }
}

class CCNil extends CCObject {
}

class CCNumber extends CCObject {
  public double value;
  public CCNumber(double value) {
    this.value = value;
  }
}

class CCString extends CCObject {
  public String value;
  public CCString(String value) {
    this.value = value;
  }
}

class CCList extends CCObject {
  public ArrayList<CCObject> list;
  CCList(CCObject... args) {
    list = new ArrayList<CCObject>(Arrays.asList(args));
  }
}
"""

  def Indent(self, string):
    return string.replace('\n', '\n  ')

  def Module(self, origin, includes, classes):
    return ''.join(includes) + ''.join(classes)

  def Include(self, origin, uri):
    raise TranslationError('includes not yet supported', origin)

  def Class(self, origin, name, bases, declarations, methods):
    if len(bases) > 1:
      raise TranslationError('multiple inheritance is not yet supported', origin)
    elif len(bases) == 1:
      base = bases[0]
    else:
      base = 'Object'

    if any(m.name == '__New__' for m in methods):
      constructor = self.Indent(
        "\npublic CC%s(CCObject... args) {"
        '\n  this.InvokeMethod("__New__", args);'
        "\n}" % (name,)
        )
    else:
      constructor = self.Indent(
        "\npublic CC%s(%s) {" % (name, ', '.join('CCObject XX' + d.name for d in declarations)) +
        ''.join(
        "\n  this.AA%s = XX%s;" % (d.name, d.name) for d in declarations
        ) +
        "\n}"
        )

    dd = ''.join(map(self.Indent, declarations))
    mm = ''.join(map(self.Indent, methods))

    ga = self.Indent(
      "\npublic CCObject GetAttribute(String name) {" +
      ''.join(
      '\n  if (name.equals("%s"))'
      "\n    return this.AA%s;" % (d.name, d.name) for d in declarations
      ) + 
      "\n  return super.GetAttribute(name);"
      "\n}")
    sa = self.Indent(
      "\npublic CCObject SetAttribute(String name, CCObject value) {" +
      ''.join(
      '\n  if (name.equals("%s"))'
      "\n    return this.AA%s = value;" % (d.name, d.name) for d in declarations
      ) + 
      "\n  return super.SetAttribute(name, value);"
      "\n}")
    im = self.Indent(
      "\npublic CCObject InvokeMethod(String name, CCObject... args) {" +
      ''.join(
      '\n  if (name.equals("%s"))'
      "\n    return this.MM%s(%s);" % (m.name, m.name, ', '.join('args[%d]' % i
      for i in range(m.arglen))) for m in methods
      ) + 
      "\n  return super.InvokeMethod(name, args);"
      "\n}")

    return '\nclass CC%s extends CC%s\n{%s\n}' % (name, base, constructor + dd + mm + ga + sa + im)

  def AttributeDeclaration(self, origin, name, type_):
    type_ = 'Object' # TODO: Change to type_ = type_ or 'Object'
    decl = StringWithData('\npublic CC%s AA%s;' % (type_, name))
    decl.name = name
    return decl

  def Declaration(self, origin, name, type_):
    type_ = 'Object' # TODO: Change to type_ = type_ or 'Object'
    return '\nCC%s XX%s;' % (type_, name)

  def StatementBlock(self, origin, statements):
    return '\n{' + ''.join(map(self.Indent, statements)) + '\n}'

  def Method(self, origin, name, args, return_type, body):
    arglen = len(args)
    args = [(n, t or 'Object') for n, t in args]
    # TODO: Change to args = ', '.join('CC%s XX%s' % (t, n) for n, t in args)
    args = ', '.join('CCObject XX%s' % n for n, _ in args)

    return_type = 'Object' # TODO: Chante to return_type = return_type or 'Object'

    m = StringWithData('\npublic CC%s MM%s(%s)%s' % (return_type, name, args, body))
    m.name = name
    m.arglen = arglen

    return m

  def Return(self, origin, expression):
    return '\nreturn %s;' % expression

  def If(self, origin, test, body, other):
    return '\nif (%s.ToBoolean())%s\nelse%s' % (test, body, other) if other else '\nif (%s.ToBoolean())%s' % (test, body)

  def While(self, origin, test, body):
    return '\nwhile (%s.ToBoolean())%s' % (test, body)

  def Break(self, origin):
    return '\nbreak;'

  def Continue(self, origin):
    return '\ncontinue;'

  def ExpressionStatement(self, origin, expression):
    return '\n%s;' % expression

  def Name(self, origin, name):
    return 'this' if name == 'this' else 'XX' + name

  def String(self, origin, value):
    return 'new CCString("%s")' % value.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')

  def Number(self, origin, value):
    return 'new CCNumber(%r)' % value

  def List(self, origin, items):
    return 'new CCList(%s)' % ', '.join(items)

  def GetAttribute(self, origin, owner, attribute):
    return '%s.GetAttribute("%s")' % (owner, attribute)

  def SetAttribute(self, origin, owner, attr, value):
    return '%s.SetAttribute("%s", %s)' % (owner, attr, value)

  def MethodCall(self, origin, owner, attribute, arguments):
    return '%s.InvokeMethod("%s"%s)' % (owner, attribute, ''.join(', ' + arg for arg in arguments))

  def Assignment(self, origin, name, value):
    return 'XX%s = %s' % (name, value)

  def New(self, origin, name, args):
    return 'new CC%s(%s)' % (name, ', '.join(args))

### Translator Tests

translation = Translator('<test>', r"""
""").Translate()

try:
  translation = Translator('<test>', r"""

include 'local:lex.ccl'

class Main
  pass

  """).Translate()
except TranslationError:
  pass
else:
  assert False, 'expected include not supported error to be raised, but nothing happened.'

translation = Translator('<test>', r"""

class Main

  var x : Main

  method Function(a, b, c)
    return x

""").Translate()


def Main():
  if len(sys.argv) == 1:
    filespec = '<stdin>'
    string = sys.stdin.read()
  elif len(sys.argv) == 2:
    filespec = sys.argv[1]
    with open(filespec) as f:
      string = f.read()
  else:
    sys.stderr.write('usage: python %s [inputfile]\n' % sys.argv[0])
    exit(1)

  sys.stdout.write(Translator(filespec, string).Translate() + '\n')


if __name__ == '__main__':
  Main()
