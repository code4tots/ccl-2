def lex(s):
  i = 0
  toks = []
  while True:
    while i < len(s) and s[i].isspace():
      i += 1

    if i >= len(s):
      break

    j = i

    if s[i] in ('(', ')', '[', ']', '.', '='):
      toks.append((s[i], None))
      i += 1
      continue

    # number
    if s[i] == '-':
      i += 1
    seendot = False
    if i < len(s) and s[i] == '.':
      seendot = True
      i += 1
    if i < len(s) and s[i].isdigit():
      while i < len(s) and s[i].isdigit():
        i += 1
      if not seendot:
        if i < len(s) and s[i] == '.':
          i += 1
        while i < len(s) and s[i].isdigit():
          i += 1
      toks.append(('number', float(s[j:i])))
      continue
    else:
      i = j

    # string
    if s.startswith(("r'", 'r"', "'", '"'), i):
      raw = False
      if s[i] == 'r':
        i += 1
        raw = True
      q = s[i:i+3] if all(s[i] == c for c in s[i:i+3]) else s[i]
      i += len(q)
      while i < len(s) and not s.startswith(q, i):
        i += 2 if not raw and s[i] == '\\' else 1
      if i >= len(s):
        raise SyntaxError("Unterminated string")
      toks.append(('string', eval(s[j:i])))
      continue

    # name
    while i < len(s) and (s[i] == '_' or s[i].isalnum()):
      i += 1
    if j < i:
      toks.append(('name', s[j:i]))
      continue

    while i < len(s) and not s[i].isspace():
      i += 1
    raise SyntaxError("Unrecognized token: " + s[j:i])
  return toks

class Parser(object):

  def __init__(self, s):
    self.toks = lex(s)
    self.i = 0

  def parse_primary_expression(self):
    type_, val = tok = self.toks[self.i]
    if type_ in ('name', 'string', 'number'):
      return {'type': type_, 'value': val}
    raise SyntaxError(tok)

  def parse_postfix_expression(self):
    expr = self.parse_primary_expression()
    while True:
      if self.i < len(self.toks) and self.toks[i] == '.':
        pass

  def parse(self):
    pass

def parse(s):
  toks = lex(s)
  i = 0
  stack = [[]]

  while i < len(toks):
    kind = None
    if toks[i][0] == '(':
      stack.extend(([], 'call'))
    elif toks[i][0] == ')':
      assert stack.pop() == 'call'
      kind = 'call'
      val = stack.pop()
    elif toks[i][0] == '[':
      stack.extend(([], 'list'))
    elif toks[i][0] == ']':
      assert stack.pop() == 'list'
      kind = 'list'
      val = stack.pop()


