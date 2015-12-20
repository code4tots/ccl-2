"""common.py

In my multiple iterations of doing this, this is the code
that seems to stay the same.
"""

def check_type(token, arg, t):
  if isinstance(t, type):
    return isinstance(arg, t)
  elif isinstance(t, tuple):
    if not isinstance(arg, tuple) or len(arg) != len(t):
      return False
    for a, tt in zip(arg, t):
      if not check_type(token, a, tt):
        return False
    return True
  elif isinstance(t, list):
    return isinstance(arg, list) and all(check_type(token, a, t[0]) for a in arg)
  elif isinstance(t, str):
    return isinstance(arg, str) and arg in set(t.split())
  else:
    raise ParseError(token, '%s is not a valid type descriptor' % t)

class Ast(object):
  def __init__(self, token, *args):
    self.token = token
    attrs = type(self).attrs
    self._args = args
    if len(attrs) != len(args):
      raise ParseError(
          token,
          'Expected %d arguments but found %d' % (
              len(attrs), len(args)))

    for i, ((name, t), arg) in enumerate(zip(attrs, args)):
      if not check_type(token, arg, t):
        raise ParseError(
            token,
            'Expected argument %d of %s to be of type %s but found %s' % (
                i, type(self).__name__, t, arg))
      setattr(self, name, arg)

  def __repr__(self):
    return '%s(%s)' % (type(self).__name__, ', '.join(map(repr, self._args)))

  def __hash__(self):
    return 0

  def __eq__(self, other):
    return type(self) == type(other) and self._args == other._args

class TranslationError(Exception):
  def __init__(self, token, message):
    self.token = token
    self.message = message

  def __str__(self):
    return (
        self.message +
        (
            '' if self.token is None else
            self.token.location_message()
        ))

class AstVisitor(object):
  def visit(self, node):
    return getattr(self, 'visit' + type(node).__name__)(node)

class ParseError(TranslationError):
  pass

class Token(object):
  def __init__(self, lexer, i, type_, value=None):
    self.lexer = lexer
    self.i = i
    self.type = type_
    self.value = value

  def location_message(self):
    a = self.i
    while a > 0 and self.lexer.s[a-1] != '\n':
      a -= 1
    b = self.i
    while b < len(self.lexer.s) and self.lexer.s[b] != '\n':
      b += 1
    lineno = self.lexer.s.count('\n', self.i) + 1
    nspce = self.i - a
    return (
        '\n## in file ' + self.lexer.filespec + ' on line ' + str(lineno) +
        '\n' + self.lexer.s[a:b] + '\n' + ' ' * nspce + '*')

SYMBOLS = (
    '(', ')', '[', ']', '{', '}', ',', '.', '@', '=', '<',
)
KEYWORDS = (
    'class', 'if', 'else', 'while', 'break', 'continue', 'return',
    'include', 'template', 'auto', 'strong', 'weak', 'self',
)

class Lexer(object):

  def __init__(self, string, filespec):
    self.s = string
    self.filespec = filespec
    self.i = 0
    self.done = False
    self.peek = self.extract()

  def next(self):
    token = self.peek
    self.peek = self.extract()
    return token

  def extract(self):
    if self.done:
      raise Exception('Already found EOF')

    while True:
      while self.i < len(self.s) and self.s[self.i].isspace():
        self.i += 1

      if self.s.startswith('#', self.i):
        while self.i < len(self.s) and self.s[self.i] != '\n':
          self.i += 1
      else:
        break

    if self.i >= len(self.s):
      self.done = True
      return Token(self, self.i, 'EOF')

    j = self.i

    # STR
    if self.s.startswith(('r"', "r'", '"', "'"), self.i):
      raw = False
      if self.s.startswith('r', self.i):
        self.i += 1
        raw = True

      if self.s.startswith(('"""', "'''"), self.i):
        quote = self.s[self.i:self.i+3]
      else:
        quote = self.s[self.i]

      self.i += len(quote)

      while not self.s.startswith(quote, self.i):
        if self.i >= len(self.s):
          raise Exception('Quote terminated')

        self.i += 2 if self.s[self.i] == '\\' else 1

      self.i += len(quote)

      s = eval(self.s[j:self.i])
      return Token(self, j, 'STR', s)

    # INT and FLOAT
    seen_dot = seen_digit = False

    if self.s.startswith(('+', '-'), self.i):
      self.i += 1

    if self.s.startswith('.', self.i):
      self.i += 1
      seen_dot = True

    while self.i < len(self.s) and self.s[self.i].isdigit():
      seen_digit = True
      self.i += 1

    if not seen_dot and self.i < len(self.s) and self.s[self.i] == '.':
      seen_dot = True
      self.i += 1

    while self.i < len(self.s) and self.s[self.i].isdigit():
      seen_digit = True
      self.i += 1

    if seen_digit:
      s = self.s[j:self.i]
      if seen_dot:
        return Token(self, j, 'FLOAT', float(s))
      else:
        return Token(self, j, 'INT', int(s))

    self.i = j

    # ID and keywords
    while self.i < len(self.s) and (
        self.s[self.i].isalnum() or self.s[self.i] == '_'):
      self.i += 1

    if j != self.i:
      s = self.s[j:self.i]
      if s in KEYWORDS:
        return Token(self, j, s)
      else:
        return Token(self, j, 'ID', s)

    # SYMBOLS
    if self.s.startswith(SYMBOLS, self.i):
      s = max(s for s in SYMBOLS if self.s.startswith(s, self.i))
      self.i += len(s)
      return Token(self, j, s)

    # ERR
    while self.i < len(self.s) and not self.s[self.i].isspace():
      self.i += 1

    s = self.s[j:self.i]
    token = Token(self, j, 'ERR', s)
    raise ParseError(token, 'Invalid token ' + s)


class Parser(object):

  def __init__(self, string, filespec):
    self.lexer = Lexer(string, filespec)

  def peek(self):
    return self.lexer.peek

  def next(self):
    return self.lexer.next()

  def at(self, type_):
    return self.peek().type == type_

  def consume(self, type_):
    if self.at(type_):
      return self.next()

  def expect(self, type_):
    if not self.at(type_):
      raise ParseError(
          self.peek(), "Expected %s but found %s" % (type_, self.peek().type))
    return self.next()

