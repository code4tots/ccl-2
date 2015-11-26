### Ast

class Ast(object):
  """
  Abstract fields:
    token # Token (to indicate location in source)
  """

class StringAst(Ast):
  def __init__(self, token, value):
    assert token.type == 'STR', token.type
    self.token = token # Token
    self.value = value # str

class NumberAst(Ast):
  def __init__(self, token, value):
    assert token.type == 'NUM', token.type
    self.token = token # Token
    self.value = value # float

class NameAst(Ast):
  def __init__(self, token, name):
    assert token.type == 'ID', token.type
    self.token = token # Token
    self.name = name # str

class AssignAst(Ast):
  def __init__(self, token, name, expr):
    assert token.type == '=', token.type
    self.token = token # Token
    self.name = name # str
    self.expr = expr # Ast

class CallAst(Ast):
  def __init__(self, token, f, args, vararg):
    self.token = token # Token
    self.f = f # Ast
    self.args = args # [Ast]
    self.vararg = vararg # Ast?

class GetAttrAst(Ast):
  def __init__(self, token, expr, attr):
    self.token = token # Token
    self.expr = expr # Ast
    self.attr = attr # str

class SetAttrAst(Ast):
  def __init__(self, token, expr, attr, val):
    self.token = token # Token
    self.expr = expr # Ast
    self.attr = attr # str
    self.val = val # Ast

class FuncAst(Ast):
  def __init__(self, token, name, args, vararg, body):
    self.token = token # Token
    self.name = name # str?
    self.args = args # [str]
    self.vararg = vararg # str?
    self.body = body # Ast

class ReturnAst(Ast):
  def __init__(self, token, expr):
    self.token = token # Token
    self.expr = expr # Ast?

class BreakAst(Ast):
  def __init__(self, token):
    self.token = token # Token

class ContinueAst(Ast):
  def __init__(self, token):
    self.token = token # Token

class WhileAst(Ast):
  def __init__(self, token, cond, body):
    self.token = token # Token
    self.cond = cond # Ast
    self.body = body # Ast

class IfAst(Ast):
  def __init__(self, token, cond, body, other):
    self.token = token # Token
    self.cond = cond # Ast
    self.body = body # Ast
    self.other = other # Ast?

class BlockAst(Ast):
  def __init__(self, token, exprs):
    self.token = token # Token
    self.exprs = exprs # [Ast]

### Lexer

SYMBOLS = tuple(reversed(sorted((
    '(', ')', '[', ']', '{', '}',
    '+', '-', '*', '/', '%', '\\', '.', ',', '=',
))))

KEYWORDS = tuple(reversed(sorted((
    'def', 'class', 'while', 'break', 'continue', 'for', 'in',
    'not', 'is', 'return', 'and', 'or', 'if', 'else',
))))

class Token(object):
  def __init__(self, lexer, i, type_, value=None):
    self.lexer = lexer
    self.i = i
    self.type = type_
    self.value = value

  def __repr__(self):
    if self.value is None:
      return self.type
    else:
      return '%s(%r)' % (self.type, self.value)

def iswordchar(c):
  return c.isalnum() or c == '_'

def lex(*args, **kwargs):
  return Lexer(*args, **kwargs).lex()

class Lexer(object):
  def __init__(self, string, filespec=None):
    self.string = string
    self.filespec = filespec
    self.done = False
    self._i = 0
    self.peek = self._next()

  def lex(self):
    tokens = []
    while not self.done:
      tokens.append(self.next())
    return tokens

  def next(self):
    token = self.peek
    self.peek = self._next()
    return token

  def _next(self):
    self.skip_spaces_and_comments()

    # EOF
    if not self._c:
      self.done = True
      return Token(self, self._i, 'EOF')

    j = self._i

    # STR
    if self._s(('r"', "r'", '"', "'")):
      raw = False
      if self._c == 'r':
        raw = True
        self._i += 1
      q = self._q(3) if self._s(('"""', "'''")) else self._c
      self._i += len(q)
      while not self._s(q):
        if not self._c:
          raise Exception('Finish your quotes')
        self._i += 2 if not raw and self._c == '\\' else 1
      self._i += len(q)
      return Token(self, j, 'STR', eval(self._p(j)))

    # NUM
    seen_dot = False
    if self._s(('+', '-')):
      self._i += 1

    if self._c == '.':
      seen_dot = True
      self._i += 1

    if self._c.isdigit():
      while self._c.isdigit():
        self._i += 1

      if not seen_dot and self._c =='.':
        self._i += 1
        while self._c.isdigit():
          self._i += 1

      return Token(self, j, 'NUM', float(self._p(j)))

    else:
      self._i = j

    # ID and KEYWORDS
    while iswordchar(self._c):
      self._i += 1

    if j != self._i:
      word = self._p(j)
      if word in KEYWORDS:
        return Token(self, j, word)
      else:
        return Token(self, j, 'ID', word)

    # SYMBOLS
    if self._s(SYMBOLS):
      sym = max(sym for sym in SYMBOLS if self._s(sym))
      self._i += len(sym)
      return Token(self, j, sym)

    # ERR
    while self._c and not self._c.isspace():
      self._i += 1
    raise Exception('Unrecognized token %r' % self._p(j))

  def skip_spaces_and_comments(self):
    while True:

      while self._c.isspace():
        self._i += 1

      if self._c == '#':
        while self._c and self._c != '\n':
          self._i += 1
        continue

      break

    while self._c.isspace() or self._c == '#':
      if self._c == '#':
        while self._c and self._c != '\n':
          self._i += 1
      else:
        self._i += 1

  @property
  def _c(self):
    return self._cc(0)

  def _cc(self, di):
    return self.string[self._i+di:self._i+di+1]

  def _s(self, prefix):
    return self.string.startswith(prefix, self._i)

  def _q(self, size):
    return self.string[self._i:self._i+size]

  def _p(self, start):
    return self.string[start:self._i]

### Parser

class Parser(object):
  def __init__(self, *args, **kwargs):
    self._lexer = Lexer(*args, **kwargs)

  @property
  def _peek(self):
    return self._lexer.peek

  def _next(self):
    return self._lexer.next()

  def _at(self, type_):
    return self._peek.type == type_

  def _consume(self, type_):
    if self._at(type_):
      return self._next()

  def _expect(self, type_):
    if not self._at(type_):
      raise Exception('Expected %r but found %r' % (type_, self._peek))
    return self._next()

  def _expression(self):
    return self._add_expression()

  def _add_expression(self):
    expr = self._mult_expression()
    while True:
      if self._at('+'):
        token = self._next()
        rhs = self._mult_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__add__'),
            [rhs], None)
        continue
      if self._at('-'):
        token = self._next()
        rhs = self._mult_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__sub__'),
            [rhs], None)
        continue
      break
    return expr

  def _mult_expression(self):
    expr = self._prefix_expression()
    while True:
      if self._at('*'):
        token = self._next()
        rhs = self._prefix_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__mul__'),
            [rhs], None)
        continue
      if self._at('/'):
        token = self._next()
        rhs = self._prefix_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__div__'),
            [rhs], None)
        continue
      if self._at('%'):
        token = self._next()
        rhs = self._prefix_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__mod__'),
            [rhs], None)
        continue
      break
    return expr

  def _prefix_expression(self):
    if self._at('+'):
      token = self._next()
      expr = self._prefix_expression()
      f = GetAttrAst(token, expr, '__pos__')
      return CallAst(token, f, [], None)

    if self._at('-'):
      token = self._next()
      expr = self._prefix_expression()
      f = GetAttrAst(token, expr, '__neg__')
      return CallAst(token, f, [], None)

    return self._postfix_expression()

  def _postfix_expression(self):
    expr = self._primary_expression()
    while True:
      if self._at('.'):
        token = self._next()
        name = self._expect('ID').value
        if self._at('='):
          token = self._next()
          val = self._expression()
          expr = SetAttrAst(token, expr, name, val)
        else:
          expr = GetAttrAst(token, expr, name)
        continue

      if self._at('['):
        token = self._next()
        args = []
        vararg = None
        while not self._at(']') and not self._at('*'):
          args.append(self._expression())
          self._consume(',')
        if self._consume('*'):
          vararg = self._expression()
        self._expect(']')
        if self._at('='):
          token = self._next()
          val = self._expression()
          f = GetAttrAst(token, expr, '__setitem__')
          # TODO: Figure out what to do about 'vararg'
          # coming after 'val'.
          expr = CallAst(token, f, args + [val], vararg)
        else:
          expr = CallAst(token, expr, args, vararg)
        continue

      break
    return expr

  def _argument_list(self):
    args = []
    vararg = None
    while self._at('ID'):
      args.append(self._next().value)
      self._consume(',')
    if self._consume('*'):
      vararg = self._expect('ID').value
    return args, vararg

  def _primary_expression(self):
    if self._consume('('):
      expr = self._expression()
      self._expect(')')
      return expr

    if self._at('{'):
      token = self._next()
      exprs = []
      while not self._consume('}'):
        exprs.append(self._expression())
      return BlockAst(token, exprs)

    if self._at('\\'):
      token = self._next()
      args, vararg = self._argument_list()
      dottoken = self._expect('.')
      body = self._expression()
      return FuncAst(
          token, None, args, vararg,
          ReturnAst(dottoken, body))

    if self._at('def'):
      token = self._next()
      name = None
      if self._at('ID'):
        name = self._next().value
      self._expect('[')
      args, vararg = self._argument_list()
      self._expect(']')
      body = self._expression()
      return FuncAst(token, name, args, vararg, body)

    if self._at('while'):
      token = self._next()
      cond = self._expression()
      body = self._expression()
      return WhileAst(token, cond, body)

    if self._at('if'):
      token = self._next()
      cond = self._expression()
      body = self._expression()
      other = None
      if self._consume('else'):
        other = self._expression()
      return IfAst(token, cond, body, other)

    if self._at('return'):
      token = self._next()
      expr = self._expression()
      return ReturnAst(token, expr)

    if self._at('continue'):
      return ContinueAst(self._next())

    if self._at('break'):
      return BreakAst(self._next())

    if self._at('ID'):
      token = self._next()
      name = token.value
      if self._at('='):
        token = self._next()
        expr = self._expression()
        return AssignAst(token, name, expr)
      else:
        return NameAst(token, token.value)

    if self._at('NUM'):
      token = self._next()
      return NumberAst(token, token.value)

    if self._at('STR'):
      token = self._next()
      return StringAst(token, token.value)

    raise Exception('Expected expression ' + repr(self._lexer.peek.type))
