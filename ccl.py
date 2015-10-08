def lex(fs, s):
  i = 0
  toks = []
  while True:
    while i < len(s) and s[i].isspace():
      i += 1

    if i >= len(s):
      break

    j = i

    if s[i] in ('(', ')', '[', ']', '{', '}', '.', ',', '='):
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
        if s[i-1] == '.':
          i -= 1
      toks.append(('num', float(s[j:i])))
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
      i += len(q)
      toks.append(('str', eval(s[j:i])))
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

  def __init__(self, filespec, string):
    self.toks = lex(filespec, string)
    self.i = 0

  def at(self, type_):
    return (any(self.at(t) for t in type_) if isinstance(type_, tuple) else
            self.i < len(self.toks) and self.peek()[0] == type_)

  def peek(self):
    return self.toks[self.i]

  def gettok(self):
    tok = self.peek()
    self.i += 1
    return tok

  def consume(self, type_):
    if self.at(type_):
      return self.gettok()

  def expect(self, type_):
    tok = self.consume(type_)
    if not tok:
      raise SyntaxError("Expected %s but found %s" % (type_, tok[0]))
    return tok

  def parse_primary_expression(self):
    if self.at(('name', 'str', 'num')):
      type_, val = self.gettok()
      return {'type': type_, 'val': val}
    elif self.consume('['):
      vals = []
      while not self.consume(']'):
        vals.append(self.parse_expression())
        self.consume(',')
      return {'type': 'list', 'vals': vals}
    elif self.consume('{'):
      pairs = []
      while not self.consume('}'):
        key = self.parse_expression()
        if self.consume(':'):
          val = self.parse_expression()
        else:
          val = None
        self.consume(',')
        pairs.append((key, val))

    raise SyntaxError(self.peek())

  def parse_postfix_expression(self):
    expr = self.parse_primary_expression()
    while True:
      if self.consume('.'):
        attr = self.expect('name')
        if self.consume('='):
          val = self.parse_expression()
          expr = {'type': 'setattr', 'owner': expr, 'attr': attr, 'val': val}
        else:
          expr = {'type': 'getattr', 'owner': expr, 'attr': attr}
      elif self.consume('='):
        val = self.parse_expression()
        if expr['type'] != 'name':
          raise SyntaxError('You can only assign to names or variables')
        expr = expr['val']
        expr = {'type': 'assign', 'target': expr, 'val': val}
      elif self.consume('('):
        args = []
        while not self.consume(')'):
          args.append(self.parse_expression())
          self.consume(',')
        expr = {'type': 'call', 'f': expr, 'args': args}
      else:
        break
    return expr

  def parse_expression(self):
    return self.parse_postfix_expression()

  def parse(self):
    exprs = []
    while self.i < len(self.toks):
      exprs.append(self.parse_expression())
    return {'type': 'module', 'exprs': exprs}

def parse(fs, s):
  return Parser(fs, s).parse()

def parse_files(filespecs):
  modules = dict()
  for filespec in filespecs:
    with open(filespec) as f:
      string = f.read()
    modules[filespec] = string
  return modules

def translate_node_to_objc(node):
  if isinstance(node, dict):
    return '@{%s}' % ', '.join(': '.join(map(translate_node_to_objc, pair)) for pair in node.items())

  if isinstance(node, (list, tuple)):
    return '@[%s]' % ', '.join(map(translate_node_to_objc, node))

  if isinstance(node, str):
    return '@"%s"' % node.replace('"', '\\"').replace('\n', '\\n')

  if isinstance(node, float):
    return '@%f' % node

  raise ValueError(type(node))

def translate_str_to_objc(filespec, string):
  return translate_node_to_objc(parse(filespec, string))

def translate_files_to_objc(filespecs):
  return translate_node_to_objc(parse_files(filespecs))

print(translate_str_to_objc('<test>', r"""
import('blarg')

a.b = 4
y = 5
1.add(4)
"""))

