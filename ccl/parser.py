from . import err
from . import lexer

def parse(source):
  return Parser(source).parse_module()

class Parser(object):
  def __init__(self, source):
    self.source = source
    self.tokens = lexer.lex(source)
    self.i = 0

  def peek(self):
    return self.tokens[self.i]

  def gettok(self):
    self.i += 1
    return self.tokens[self.i-1]

  def at(self, type_):
    return self.peek().type == type_

  def consume(self, type_):
    if self.at(type_):
      return self.gettok()

  def expect(self, type_):
    if self.at(type_):
      return self.gettok()
    else:
      raise err.Err(
          'Expected token of type %r but found %r' % (
              type_, self.peek()),
          self.peek())

  ##############

  def parse_module(self):
    start = self.peek()
    incs = []
    clss = []
    funcs = []
    decls = []
    while not self.at('EOF'):
      token = self.peek()
      if self.consume('include'):
        uri = eval((self.consume('STR') or self.consume('CHR')).value)
        incs.append(('include', token, uri))
      elif self.at('class'):
        clss.append(self.parse_class())
      elif self.at('fn'):
        funcs.append(self.parse_func())
      elif self.at('var'):
        decls.append(self.parse_decl())
      elif self.consume('STR') or self.consume('CHR'):
        pass  # string or char style comments
      else:
        raise err.Err(
            'Expected function, declaration, class or include', self.peek())
    return ('module', start, incs, clss, funcs, decls)

  def parse_class(self):
    token = self.expect('class')
    name = self.expect('ID').value
    if not self.at('{'):
      base = self.expect('ID').value
    else:
      base = 'Object'
    members = []
    methods = []
    self.expect('{')
    while not self.consume('}'):
      if self.at('var'):
        members.append(self.parse_decl())
      elif self.at('fn'):
        methods.append(self.parse_func())
      elif self.consume('STR') or self.consume('CHR'):
        pass  # string or char style comments
      else:
        raise err.Err('Expected method or declaration', self.peek())
    return ('class', token, name, base, members, methods)

  def parse_decl(self):
    token = self.expect('var')
    name = self.expect('ID').value
    if self.consume('='):
      expr = self.parse_expr()
    else:
      expr = None
    return ('decl', token, name, expr)

  def parse_func(self):
    token = self.expect('fn')
    if not self.at('['):
      name = self.expect('ID').value
    else:
      name = None
    argnames = []
    varargname = None
    self.expect('[')
    while not self.consume(']'):
      if self.consume('*'):
        varargname = self.expect('ID').value
        self.expect(']')
        break
      else:
        argnames.append(self.expect('ID').value)
        self.consume(',')
    body = self.parse_block()
    return ('func', token, name, argnames, varargname, body)

  def parse_block(self):
    token = self.expect('{')
    stmts = []
    while not self.consume('}'):
      stmts.append(self.parse_statement())
    return ('block', token, stmts)

  def parse_statement(self):
    token = self.peek()
    if self.at('{'):
      return self.parse_block()
    elif self.consume('break'):
      return ('break', token)
    elif self.consume('continue'):
      return ('continue', token)
    elif self.consume('while'):
      cond = self.parse_expr()
      body = self.parse_block()
      return ('while', token, cond, body)
    elif self.consume('if'):
      cond = self.parse_expr()
      body = self.parse_block()
      if self.consume('else'):
        other = self.parse_statement()
      else:
        other = None
      return ('if', token, cond, body, other)
    elif self.consume('for'):
      name = self.expect('ID').value
      self.expect('in')
      container = self.parse_expr()
      body = self.parse_block()
      return ('for', token, name, container, body)
    elif self.consume('return'):
      expr = self.parse_expr()
      return ('return', token, expr)
    elif self.at('var'):
      return self.parse_decl()
    else:
      return ('expr', token, self.parse_expr())

  def parse_expr(self):
    return self.parse_or_expression()

  def parse_or_expression(self):
    e = self.parse_and_expression()
    while True:
      token = self.peek()
      if self.consume('or'):
        r = self.parse_and_expression()
        e = ('or', token, e, r)
      else:
        break
    return e

  def parse_and_expression(self):
    e = self.parse_equality_expression()
    while True:
      if self.consume('and'):
        r = self.parse_equality_expression()
        e = ('and', token, e, r)
      else:
        break
    return e

  def parse_equality_expression(self):
    e = self.parse_inequality_expression()
    while True:
      if self.consume('=='):
        r = self.parse_inequality_expression()
        e = makemcall2(token, e, '_eq_', [r])
      elif self.consume('!='):
        r = self.parse_inequality_expression()
        e = makemcall2(token, e, '_ne_', [r])
      elif self.consume('is'):
        if self.consume('not'):
          r = self.parse_inequality_expression()
          e = ('is_not', token, e, r)
        else:
          r = self.parse_inequality_expression()
          e = ('is', token, e, r)
      else:
        break
    return e

  def parse_inequality_expression(self):
    e = self.parse_additive_expression()
    while True:
      token = self.peek()
      if self.consume('<'):
        r = self.parse_additive_expression()
        e = makemcall2(token, e, '_lt_', [r])
      elif self.consume('<='):
        r = self.parse_additive_expression()
        e = makemcall2(token, e, '_le_', [r])
      elif self.consume('>'):
        r = self.parse_additive_expression()
        e = makemcall2(tokne, e, '_gt_', [r])
      elif self.consume('>='):
        r = self.parse_additive_expression()
        e = makemcall2(token, e, '_ge_', [r])
      else:
        break
    return e

  def parse_additive_expression(self):
    e = self.parse_multiplicative_expression()
    while True:
      token = self.peek()
      if self.consume('+'):
        r = self.parse_multiplicative_expression()
        e = makemcall2(token, e, '_add_', [r])
      elif self.consume('-'):
        r = self.parse_multiplicative_expression()
        e = makemcall2(token, e, '_sub_', [r])
      else:
        break
    return e

  def parse_multiplicative_expression(self):
    e = self.parse_prefix_expression()
    while True:
      token = self.peek()
      if self.consume('*'):
        r = self.parse_prefix_expression()
        e = makemcall2(token, e, '_mul_', [r])
      elif self.consume('/'):
        r = self.parse_prefix_expression()
        e = makemcall2(token, e, '_div_', [r])
      elif self.consume('%'):
        r = self.parse_prefix_expression()
        e = makemcall2(token, e, '_mod_', [r])
      else:
        break
    return e

  def parse_prefix_expression(self):
    token = self.peek()
    if self.consume('-'):
      r = self.parse_postfix_expression()
      return makemcall2(token, r, '_neg_', [])
    elif self.consume('not'):
      r = self.parse_postfix_expression()
      return makemcall2(token, r, '_not_', [])
    else:
      return self.parse_postfix_expression()

  def parse_postfix_expression(self):
    e = self.parse_primary_expression()
    while True:
      token = self.peek()
      if self.consume('.'):
        name = self.expect('ID').value
        if self.at('['):
          args = self.parse_args()
          e = makemcall(token, e, name, args)
        elif self.consume('='):
          v = self.parse_expr()
          e = ('setattr', token, e, name, v)
        else:
          e = ('getattr', token, e, name)
      elif self.at('['):
        args = self.parse_args()
        e = makemcall(token, e, '_call_', args)
      else:
        break
    return e

  def parse_primary_expression(self):
    token = self.peek()
    if self.consume('('):
      e = self.parse_expr()
      self.expect(')')
      return e
    elif self.at('ID'):
      name = self.expect('ID').value
      if self.consume('='):
        expr = self.parse_expr()
        return ('setvar', token, name, expr)
      else:
        return ('getvar', token, name)
    elif self.at('fn'):
      return self.parse_func()
    elif self.consume('self'):
      return ('self', token)
    elif self.consume('true'):
      return ('true', token)
    elif self.consume('false'):
      return ('false', token)
    elif self.at('NUM'):
      return ('num', token, float(self.expect('NUM').value))
    elif self.at('STR'):
      return ('str', token, eval(self.expect('STR').value))
    else:
      raise err.Err('Expected expression', token)

  def parse_args(self):
    token = self.expect('[')
    args = []
    vararg = None
    while not self.consume(']'):
      if self.consume('*'):
        vararg = self.parse_expr()
        self.expect(']')
        break
      else:
        args.append(self.parse_expr())
        self.consume(',')
    return makeargs(token, args, vararg)

def makeargs(token, args, vararg=None):
  return ('args', token, args, vararg)

def makemcall(token, f, name, args):
  return ('mcall', token, f, name, args)

def makemcall2(token, f, name, args):
  return makemcall(token, f, name, makeargs(token, args))
