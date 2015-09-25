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



