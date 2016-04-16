import re

from . import err

def compile_re(pattern):
  return re.compile(pattern, re.DOTALL|re.MULTILINE)

class Source(object):
  def __init__(self, filespec, data):
    self.filespec = filespec
    self.data = data

symbols = tuple(reversed(sorted((
    '(', ')', '[', ']', '{', '}', '.', ',', ';', '=', '==', ':',
    '+', '-', '*', '/', '%', '$', '<', '>', '<=', '>=', '!',
    '+=', '-=', '*=', '/=', '%='))))
keywords = (
    'fn', 'return', 'if', 'else', 'while', 'break', 'continue',
    'var', 'include', 'extern', 'new', 'true', 'false', 'self',
    'class', 'for', 'in', 'is', 'not', 'and', 'or', 'nil')
whitespace_pattern = compile_re(r'(?:\s|#.*?$)*')
err_pattern = compile_re(r'\S+')
token_table = tuple((type_, compile_re(pattern)) for type_, pattern in
    [
      ('STR', r'"""(?:\\|(?!\"\"\").)*"""'),
      ('STR', r"'''(?:\\|(?!\'\'\').)*'''"),
      ('STR', r'"(?:\\|(?!\").)*"'),
      ('STR', r"'(?:\\|(?!\').)*'"),
      ('STR', r'r""".*?"""'),
      ('STR', r"r'''.*?'''"),
      ('STR', r'r".*?"'),
      ('STR', r"r'.*?'"),
      ('NUM', r'\d+\.\d*'),
      ('NUM', r'\d*\.\d+'),
      ('NUM', r'\d+'),
    ] +
    [(kw, r'\b%s\b' % kw) for kw in keywords] +
    [(sym, re.escape(sym)) for sym in symbols] +
    [('ID', r'\w+')])

class Token(object):
  def __init__(self, type_, value, i, source):
    self.type = type_
    self.value = value
    self.i = i
    self.source = source

  def __repr__(self):
    return '(%s, %r)@%d' % (self.type, self.value, self.i)

  def lineno(self):
    return 1 + self.source.data.count('\n', 0, self.i)

  def colno(self):
    return self.i - self.source.data.rfind('\n', 0, self.i)

  def line(self):
    a = self.source.data.rfind('\n', 0, self.i) + 1
    b = self.source.data.find('\n', self.i)
    if b == -1:
      b = len(self.source.data)
    return self.source.data[a:b]

def lex(source):
  s = source.data
  tokens = []
  i = whitespace_pattern.match(s).end()
  while i < len(s):
    for type_, pattern in token_table:
      m = pattern.match(s, i)
      if m:
        tokens.append(Token(type_, m.group(), i, source))
        i = m.end()
        break
    else:
      raise err.Err(
          'Unrecognized token',
          Token('ERR', err_pattern.match(s, i).group(), i, source))
    i = whitespace_pattern.match(s, i).end()
  tokens.append(Token('EOF', None, i, source))
  return tokens
