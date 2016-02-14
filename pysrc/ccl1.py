"""ccl1.py

CCL tier 1.

"""


class Token(object):

  def __init__(self, lexer, i, type, value):
    self.lexer = lexer
    self.i = i
    self.type = type
    self.value = value

  def get_line_number(self):
    return 1 + self.lexer.string.count('\n', self.i)

  def get_location_string(self):
    a = self.lexer.string.rfind('\n', 0, self.i) + 1
    b = self.lexer.string.find('\n', self.i)
    if b == -1:
      b = len(self.lexer.string)
    return "file %d on line %d\n%s\n%s*" % (
        self.lexer.filespec,
        self.get_line_number(),
        self.lexer.string[a:b],
        ' ' * (self.i-a))


class Lexer(object):

  def __init__(self, string, filespec):
    self.string = string
    self.filespec = filespec
    self.tokens = []
    self.peek = None
    self.done = False
    self.pos = 0
    self.next()

  def next(self):
    token = self.peek
    self.peek = self.extract()
    self.tokens.append(self.peek)
    return token

  def extract(self):
    self.skip_spaces_and_comments()

    if not self.more():
      self.done = True
      return self.make_token(self.pos, "EOF")

    start = self.pos

    if self.starts_with("r'", "'", 'r"', '"'):
      raw = False
      if self.starts_with('r'):
        self.pos += 1
        raw = True
      quote = (
          self.slice(3) if
          self.starts_with('"""', "'''") else
          self.slice(1))
      self.pos += len(quote)
