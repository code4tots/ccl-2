class Source(object):
  def __init__(self, filespec, string):
    self.filespec = filespec
    self.string = string

class Token(object):
  def __init__(self, source, i, type_, value=None):
    self.source = source
    self.i = i
    self.type = type_
    self.value = value

SYMBOLS = {'=', '\\', '\\\\', '+', '-', '*', '/', '%'}
KEYWORDS = {'return', 'if', 'else', 'while', 'break', 'continue'}

def lex(source):
  s = source.string
  i = 0
  toks = []
  while i < len(s):
    while i < len(s) and (s[i].isspace() or s[i] == '#'):
      if s[i] == '#':
        while i < len(s) and s[i] != '\n':
          i += 1
      else:
        i += 1

    if i >= len(s):
      break

    j = i

    # string
    if s.startswith(('r"', "r'", "'", '"'), i):
      raw = False
      if s[i] == 'r':
        raw = True
        i += 1
      if s.startswith(('"""', "'''"), i):
        quote = s[i:i+3]
      else:
        quote = s[i]
      i += len(quote)
      while not s.startswith(quote, i):
        if i >= len(s):
          raise Exception('Unterminated string literal')
        i += 2 if s[i] == '\\' else 1
      i += len(quote)
      toks.append(Token(source, j, 'STRING', eval(s[j:i])))
      continue

    # int and float
    seen_dot = False
    seen_digit = False
    while i < len(s) and s[i].isdigit():
      seen_digit = True
      i += 1
    if i < len(s) and s[i] == '.':
      seen_dot = True
      i += 1
    while i < len(s) and s[i].isdigit():
      seen_digit = True
      i += 1
    if seen_digit:
      if seen_dot:
        toks.append(Token(source, j, 'FLOAT', float(s[j:i])))
      else:
        toks.append(Token(source, j, 'INT', int(s[j:i])))
      continue
    else:
      i = j

    # identifier and keyword
    while i < len(s) and (s[i].isalnum() or s[i] == '_'):
      i += 1

    if j != i:
      name = s[j:i]
      if name in KEYWORDS:
        toks.append(Token(source, j, name))
      else:
        toks.append(Token(source, j, 'ID', name))
      continue

    # symbol
    if any(s.startswith(sym, i) for sym in SYMBOLS):
      sym = max(sym for sym in SYMBOLS if s.startswith(sym, i))
      tok = Token(source, i, sym)
      i += len(sym)
      toks.append(tok)
      continue

    # err
    while i < len(s) and not s[i].isspace():
      i += 1
    raise Exception('Invalid token: ' + s[j:i])

  return toks

pairs = [(token.type, token.value) for token in lex(Source('<test>', """
'hi'
55
6.60
hi
return
*
"""))]

assert pairs == [
  ('STRING', 'hi'),
  ('INT', 55),
  ('FLOAT', 6.6),
  ('ID', 'hi'),
  ('return', None),
], pairs

