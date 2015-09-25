"""translator.py"""

import sys

### Lexer

SYMBOLS = ('.', '=', '(', ')', '[', ']', ',', ';', ':',)
KEYWORDS = (
  'class', 'lambda',
  'pass',
  'var',
  'return',
  'while', 'break', 'continue',
  'if', 'else',
)


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

  def __init__(self, origin, type, value=None):
    self.origin = origin
    self.type = type
    self.value = value

  def __eq__(self, other):
    return self.type == other.type and self.value == other.value

  def __repr__(self):
    return 'Token(%r,%r)' % (self.type, self.value)


class CclError(Exception):

  def __init__(self, origin, message):
    super(CclError, self).__init__(message + '\n' + origin.LocationMessage())


class LexError(CclError):
  pass


def Lex(filespec, string):
  s = string
  i = 0
  j = 0
  indent_stack = ['']
  depth = 0
  tokens = []

  def MakeOrigin():
    return Origin(filespec, s, j)

  def MakeToken(type, value=None):
    return Token(MakeOrigin(), type, value)

  while True:
    "skips newlines iff depth > 0"
    while i < len(s) and ((s[i].isspace() and (depth or s[i] != '\n')) or s[i] == '#'):
      if s[i] == '#':
        while i < len(s) and s[i] != '\n':
          i += 1
      else:
        i += 1

    if i >= len(s):
      break

    j = i

    if s[i] == '\n':
      i += 1
      tokens.append(MakeToken('Newline'))
      while True:
        j = i
        while i < len(s) and s[i].isspace() and s[i] != '\n':
          i += 1
        if i < len(s) and s[i] == '#':
          while i < len(s) and s[i] != '\n':
            i += 1
        if i >= len(s) or not s[i].isspace():
          break
        i += 1
      if i < len(s):
        indent = s[j:i]
        if indent == indent_stack[-1]:
          pass
        elif indent.startswith(indent_stack[-1]):
          tokens.append(MakeToken('Indent'))
          tokens.append(MakeToken('Newline'))
          indent_stack.append(indent)
        elif indent in indent_stack:
          while indent != indent_stack[-1]:
            tokens.append(MakeToken('Dedent'))
            tokens.append(MakeToken('Newline'))
            indent_stack.pop()
        else:
          raise LexError(MakeOrigin(), 'Invalid indent: ' + repr(indent))

    elif s[i].isdigit() or s[i] == '.' and s[i+1:i+2].isdigit():
      while i < len(s) and s[i].isdigit():
        i += 1
      if i < len(s) and s[i] == '.':
        i += 1
      while i < len(s) and s[i].isdigit():
        i += 1
      tokens.append(MakeToken('Number', float(s[j:i])))

    elif s.startswith(('r"', "r'", '"', "'"), i):
      raw = False
      if s[i] == 'r':
        i += 1
        raw = True
      quote = s[i:i+3] if s.startswith(('"""', "'''"), i) else s[i]
      i += len(quote)
      while not s.startswith(quote, i):
        if i >= len(s):
          raise LexError(MakeOrigin(), "Missing quotes for: " + quote)
        i += 2 if not raw and s[i] == '\\' else 1
      i += len(quote)
      tokens.append(MakeToken('String', eval(s[j:i])))

    elif s.startswith(SYMBOLS, i):
      symbol = max(symbol for symbol in SYMBOLS if s.startswith(symbol, i))
      if symbol in ('(', '{', '['):
        depth += 1
      elif symbol in (')', '}', ']'):
        depth -= 1
      i += len(symbol)
      tokens.append(MakeToken(symbol))

    elif s[i].isalnum() or s[i] == '_':
      while s[i].isalnum() or s[i] == '_':
        i += 1
      word = s[j:i]
      if word in KEYWORDS:
        tokens.append(MakeToken(word))
      else:
        tokens.append(MakeToken('Name', word))

    else:
      while i < len(s) and not s[i].isspace():
        i += 1
      raise LexError(MakeOrigin(), "Unrecognized token: " + s[j:i])

  while indent_stack[-1] != '':
    tokens.append(MakeToken('Dedent', None))
    indent_stack.pop()

  tokens.append(MakeToken('End'))

  return tokens

### Lexer Tests

origin = Origin('<test>', """
hello world!
""", 1)

assert origin.Line() == 'hello world!', repr(origin.Line())

assert origin.LocationMessage() == """in <test> on line 2 column 1
hello world!
*
""", repr(origin.LocationMessage())

tokens = Lex('<test>', """
"hello".Print()
""")

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

tokens = Lex('<test>', """
i = 0
while i.LessThan(10)
  i.Print()
  i = i.Add(1)
""")

assert (
    tokens ==
    [
        Token(None, 'Newline', None),

        Token(None, 'Name', 'i'),
        Token(None, '=', None),
        Token(None, 'Number', 0.0),
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
  Lex('<test>', '!@#')
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


# TODO: include origin data in parse tree.
# I've already set origin_pointers to facilitate this,
# but actually testing it is going to be a bit more annoying.
class Parser(object):

  def __init__(self, filespec, string):
    self.filespec = filespec
    self.string = string
    self.tokens = Lex(filespec, string)
    self.i = 0

  def GetToken(self):
    token = self.tokens[self.i]
    self.i += 1
    return token

  def Peek(self, lookahead=0):
    return self.tokens[self.i + lookahead]

  def At(self, type, origin_pointer=None):
    if self.Peek().type == type:
      if origin_pointer:
        origin_pointer[0] = self.Peek().origin
      return True

  def Consume(self, type, origin_pointer=None):
    if self.At(type, origin_pointer):
      return self.GetToken()

  def Expect(self, type, origin_pointer=None):
    if not self.At(type, origin_pointer):
      raise ParseError(self.Peek(0).origin, 'Expected %s but found %s' % (type_, self.Peek(0).type))
    return self.GetToken()

  def EatStatementDelimiters(self):
    while self.Consume('Newline') or self.Consume(';'):
      pass

  def ParseModule(self):
    statements = []
    origin = self.Peek().origin
    while not self.Consume('End'):
      statements.append(self.ParseStatement())
      self.EatStatementDelimiters()
    return {'type': 'Module', 'statements': statements}

  def ParseStatement(self):
    origin_pointer = [None]
    if self.Consume('if', origin_pointer):
      test = self.ParseExpression()
      body = self.ParseStatementBlock()
      other = None
      if self.Consume('else'):
        if self.At('if'):
          other = self.ParseStatement()
        else:
          other = self.ParseStatementBlock()
      return {'type': 'If', 'test': test, 'body': body, 'other': other}
    elif self.Consume('while', origin_pointer):
      test = self.ParseExpression()
      body = self.ParseStatementBlock()
      return {'type': 'While', 'test': test, 'body': body}
    elif self.Consume('break', origin_pointer):
      return {'type': 'Break'}
    elif self.Consume('continue', origin_pointer):
      return {'type': 'Continue'}
    elif self.Continue('return', origin_pointer):
      expr = None
      if not self.At('Newline'):
        expr = self.ParseExpression()
      return {'type': 'Return', 'expression': expr}
    else:
      expr = self.ParseExpression()
      return {'type': 'Expression', 'expression': expr}

  def ParseStatementBlock(self):
    origin_pointer = [None]
    self.EatStatementDelimiters()
    self.Expect('Indent', origin_pointer)
    while not self.Consume('Dedent'):
      statements.append(self.ParseStatement())
      self.EatStatementDelimiters()
    self.EatStatementDelimiters()
    return {'type': 'Block', 'statements': statements}

  def ParseExpression(self):
    return self.ParseAssignExpression()

