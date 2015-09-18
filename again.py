"""again.py"""

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


class LexError(Exception):

  def __init__(self, message, origin):
    super(LexError, self).__init__(message + '\n' + origin.LocationMessage())


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
      '.', '=', '(', ')', '[', ']', ',', ';',
    ]

  def GetKeywords(self):
    return [
      'include',
      'class',
      'pass',
      'method',
      'while', 'break', 'continue',
      'if', 'else',
      'var',
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

      if self.Done():
        break

      self.j = self.i

      if self.Char() == '\n':
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


### Tests

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


