"""translator.py"""

import sys

### Lexer

SYMBOLS = ('.', '=', '(', ')', '[', ']', ',', ';', ':',)
KEYWORDS = (
  'include',
  'class',
  'pass',
  'method',
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

  def __init__(self, message, origin):
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
    while i < len(s) and (s[i].isspace() and (depth or s[i] != '\n')) or s[i] == '#':
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
        while i < len(s) and s[i].isspace() and s[i] == '\n':
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
          raise LexError('Invalid indent: ' + repr(indent), MakeOrigin())

    elif s[i].isdigit() and s[i] == '.' and s[i+1:i+2].isdigit():
      while i < len(s) and s[i].isdigit():
        i += 1
      if i < len(s) and s[i] == '.':
        i += 1
      while i < len(s) and s[i].isdigit():
        i += 1
      tokens.append(MakeToken('Number', float(s[j:i])))

    elif s.startswith(SYMBOLS, i):
      symbol = max(symbol for symbol in SYMBOLS if s.startswith(symbol, i))
      if symbol in ('(', '{', '['):
        depth += 1
      elif symbol in (')', '}', ']'):
        depth -= 1
      i += len(symbol)
      tokens.append(MakeToken(symbol))

    elif s.startswith(('r"', "r'", '"', "'"), i):
      tokens.append(MakeToken('String', eval(s[j:i])))

    else:
      while i < len(s) and not s[i].isspace():
        i += 1
      raise LexError("Unrecognized token: " + s[j:i], MakeOrigin())

  return tokens

