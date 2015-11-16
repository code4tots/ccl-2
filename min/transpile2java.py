# TODO: Better error handling. On everything.
KEYWORDS = (
    'true', 'false', 'nil',
    'while', 'break', 'continue',
    'if', 'else', 'and', 'or',
    'include',
)

SYMBOLS = (
    '(', ')', '[', ']', '{', '}', '.', ',',
    '*', '/', '%', '+', '-',
)

BINOPS = {
    '+': '__add__',
    '-': '__sub__',
    '*': '__mul__',
    '/': '__div__',
    '%': '__mod__',
}

PREOPS = {
    '+': '__pos__',
    '-': '__neg__',
}

def extract_indent(line):
  i = 0
  while i < len(line) and line[i].isspace():
    i += 1
  return line[:i]


def split_line_into_tokens(line):
  tokens = []
  i = 0

  while True:

    while i < len(line) and line[i].isspace():
      i += 1

    if i >= len(line):
      break

    if line[i] in ('"', "'"):
      j = i + 1
      while j < len(line) and line[j] != line[i]:
        j += 2 if line[j] == '\\' else 1
      tokens.append(('STRING', eval(line[i:j])))
      i = j
    elif any(line.startswith(s, i) for s in SYMBOLS):
      s = max(s for s in SYMBOLS if line.startswith(s, i))
      i += len(s)
      tokens.append(s)
    elif line[i].isalpha() or line[i] == '_':
      j = i + 1
      while j < len(line) and (line[j].isalnum() or line[j] == '_'):
        j += 1
      val = line[i:j]
      if val in KEYWORDS:
        tokens.append(val)
      else:
        tokens.append(('NAME', line[i:j]))
      i = j
    elif line[i].isdigit() or line[i] in '+-' and line[i+1:i+2].isdigit():
      j = i + 1
      while j < len(line) and (line[j].isdigit() or line[j] == '.'):
        j += 1
      tokens.append(('NUMBER', float(line[i:j])))
      i = j
    else:
      j = i + 1
      while j < len(line) and not line[j].isspace():
        j += 1
      raise SyntaxError('Invalid token %r' % line[i:j])

  return tokens 


class Lexer(object):

  def init(self, s):
    self.s = s
    return self

  def lex(self):
    # TODO: Iterate over logical lines, not physical lines.
    tokens = []
    indent_stack = ['']
    stripped_lines = [line.rstrip() for line in self.s.splitlines()]
    lines = [line for line in stripped_lines if line]
    for line in lines:
      indent = extract_indent(line)
      if indent in indent_stack:
        while indent != indent_stack[-1]:
          tokens.append('DEDENT')
          indent_stack.pop()
      elif indent.startswith(indent_stack[-1]):
        tokens.append('INDENT')
        indent_stack.append(indent)
      else:
        raise SyntaxError('Indent error')

      tokens.extend(split_line_into_tokens(line))
      tokens.append('NEWLINE')

    while '' != indent_stack[-1]:
      tokens.append('DEDENT')
      indent_stack.pop()

    tokens.append('EOF')

    return tokens


def lex(s):
  return Lexer().init(s).lex()


class Parser(object):

  def init(self, s):
    self.s = s
    self.i = 0
    self.tokens = Lexer().init(s).lex()
    return self

  def peek(self):
    return self.tokens[self.i]

  def next(self):
    tok = self.peek()
    self.i += 1
    return tok

  def nexttype(self):
    return self.next() if isinstance(self.peek(), str) else self.next()[0]

  def nextval(self):
    return self.next()[1] # Haha dirty. But all token names are > length 1.

  def at(self, type_):
    tok = self.peek()
    return isinstance(tok, str) and tok == type_ or tok[0] == type_

  def expect(self, type_):
    if not self.at(type_):
      raise SyntaxError('Expected %r but found %r' % (type_, self.peek()))
    return self.nextval()

  def consume(self, type_):
    if self.at(type_):
      return self.next()

  def parse_module(self):
    stmts = []
    while not self.at('EOF'):
      stmts.append(self.parse_statement())
    return ('module', ('block', tuple(stmts)))

  def parse_statement(self):
    if self.consume('pass'): # ()
      self.expect('NEWLINE')
      return ('pass', ())
    elif self.at('if'): # (expr, stmt, stmt)
      return self.parse_if_statement()
    elif self.consume('while'): # (expr, stmt)
      cond = self.parse_expression()
      self.expect('NEWLINE')
      body = self.parse_block_statement()
      return ('while', (cond, body))
    else: # expr
      expr = self.parse_expression()
      self.expect('NEWLINE')
      return ('expr', expr)

  def parse_if_statement(self):
    self.expect('if')
    cond = self.parse_expression()
    self.expect('NEWLINE')
    body = self.parse_block_statement()
    other = ('pass', ())
    if self.consume('else'):
      if self.at('if'):
        other = self.parse_if_statement()
      else:
        other = self.parse_block_statement()
    return ('if', (cond, body, other))

  def parse_block_statement(self):
    self.expect('INDENT')
    stmts = []
    while not self.consume('DEDENT'):
      stmts.append(self.parse_statement())
    return ('block', tuple(stmts))

  def parse_expression(self):
    return self.parse_additive_expression()

  def parse_additive_expression(self):
    expr = self.parse_multiplicative_expression()
    while True:
      if self.at('+') or self.at('-'):
        method = BINOPS[self.nexttype()]
        rhs = self.parse_multiplicative_expression()
        expr = ('call-method', (expr, method, (rhs,)))
      else:
        break

  def parse_multiplicative_expression(self):
    expr = self.parse_prefix_expression()
    while True:
      if self.at('*') or self.at('/') or self.at('%'):
        method = BINOPS[self.nexttype()]
        rhs = self.parse_prefix_expression()
        expr = ('call-method', (expr, method, (rhs,)))
      else:
        break

  def parse_prefix_expression(self):
    if self.at('+') or self.at('-'):
      method = PREOPS[self.nexttype()]
      rhs = self.parse_prefix_expression()
      return ('call-method', (rhs, method, ()))
    return self.parse_postfix_expression()

  def parse_postfix_expression(self):
    expr = self.parse_primary_expression()
    while True:
      if self.consume('.'):
        attr = self.expect('NAME')
        if self.at('('):
          args = self.parse_args()
          expr = ('call-method', (expr, attr, args))
        elif self.at('='):
          val = self.parse_expression()
          expr = ('call-method', (expr, '__setattr__', (attr, val)))
        else:
          expr = ('call-method', (expr, '__getattr__', (attr,)))
      elif self.at('('):
        args = self.parse_args()
        expr = ('call-method', (expr, '__call__', args))
      elif self.consume('['):
        i = self.parse_expression()
        self.expect(']')
        if self.consume('='):
          val = self.parse_expression()
          expr = ('call-method', (expr, '__getitem__', (i, val)))
        else:
          expr = ('call-method', (expr, '__setitem__', (i,)))
      else:
        break
    return expr

  def parse_primary_expression(self):
    if self.at('NAME'):
      name = self.nextval()
      if self.consume('='):
        return ('assign', (name, self.parse_expression()))
      else:
        return ('id', name)
    elif self.at('NUMBER'):
      return ('num', self.nextval())
    elif self.at('STRING'):
      return ('str', self.nextval())
    elif self.consume('('):
      expr = self.parse_expression()
      self.expect(')')
      return expr
    elif self.consume('true'):
      return ('true', ())
    elif self.consume('false'):
      return ('false', ())
    elif self.consume('nil'):
      return ('nil', ())
    elif self.consume('['):
      items = []
      while not self.consume(']'):
        items.append(self.parse_expression())
        self.consume(',')
      return ('list', tuple(items))
    elif self.consume('{'):
      items = []
      while not self.consume('}'):
        key = self.parse_expression()
        val = ('nil', ())
        if self.consume(':'):
          val = self.parse_expression()
        self.consume(',')
        items.append((key, val))
        return ('dict', tuple(items))
    else:
      raise SyntaxError('Expected expression but found %r' % self.peek())

  def parse_args(self):
    self.expect('(')
    args = []
    while not self.consume(')'):
      args.append(self.parse_expression())
      self.consume(',')
    return args



def parse(s):
  return Parser().init(s).parse_module()


tokens = lex(r"""
""")
assert tokens == ['EOF'], tokens

tokens = lex(r"""
while true
  print(1)
""")
assert tokens == [
    'while', 'true', 'NEWLINE',
    'INDENT',
        ('NAME', 'print'), '(', ('NUMBER', 1), ')', 'NEWLINE',
    'DEDENT',
    'EOF',
], tokens

node = parse(r"""
""")
assert node == ('module', ('block', (
)))
