# TODO: Clean up this crap too.
# By clean up, I mean, think through the design more.
import os.path

from lex import Lexer


class Parser(object):

  ## public

  def parse(self, filespec, string):
    self.init(filespec, string)
    return self.parse_module(self.deduce_module_name(filespec))

  ## private

  def deduce_module_name(self, filespec):
    filespec = os.path.basename(filespec)
    if filespec.endswith('.ccl'):
      filespec = filespec[:-4]
    return filespec

  def init(self, filespec, string):
    self.tokens = Lexer().lex(filespec, string)
    self.position = 0
    self.module_vars = set()
    self.func_vars = set()
    return self

  def peek(self):
    return self.tokens[self.position]

  def at(self, type_):
    return type_ == self.peek().type

  def next(self):
    token = self.peek()
    self.position += 1
    return token

  def consume(self, type_):
    if self.at(type_):
      return self.next()

  def expect(self, type_):
    token = self.consume(type_)
    if not token:
      # TODO: Better error message (e.g. include location)
      raise SyntaxError('Expected %s but found %s' % (type_, self.peek()))
    return token

  def skip_newlines(self):
    while self.consume('NEWLINE'):
      pass

  ## Actual parsing methods.

  def parse_module(self, module_name):
    classes = []
    includes = []
    funcs = []
    vars_ = []
    stmts = []
    self.skip_newlines()
    while not self.at('EOF'):
      if self.at('include'):
        includes.append(self.parse_include())
      elif self.at('class'):
        classes.append(self.parse_class())
      elif self.consume('var'):
        while not self.consume('NEWLINE'):
          vars_.append(self.expect('NAME').value)
          if vars_[-1] in vars_[:-1]:
            raise SyntaxError('Duplicate module var %r' % vars_[-1])
          self.consume(',')
      elif self.at('def'):
        funcs.append(self.parse_method())
      else:
        stmts.append(self.parse_statement())
      self.skip_newlines()
    return {
        'type': 'module',
        'name': module_name,
        'classes': classes,
        'includes': includes,
        'funcs': funcs,
        'vars': vars_,
        'stmts': stmts,
    }

  def parse_include(self):
    self.expect('include')
    module_name = self.expect('NAME').value
    self.expect('NEWLINE')
    return module_name

  def parse_class(self):
    methods = []
    attrs = []
    bases = []
    self.expect('class')
    class_name = self.expect('NAME').value
    while not self.consume('NEWLINE'):
      bases.append(self.expect('NAME').value)
    self.expect('INDENT')
    self.skip_newlines()
    while not self.consume('DEDENT'):
      if self.consume('pass'):
        self.expect('NEWLINE')
      elif self.at('def'):
        methods.append(self.parse_method())
      elif self.consume('var'):
        while not self.consume('NEWLINE'):
          attrs.append(self.expect('NAME').value)
          if attrs[-1] in attrs[:-1]:
            raise SyntaxError('Duplicate class attribute %r' % attrs[-1])
          self.consume(',')
      else:
        raise SyntaxError()
      self.skip_newlines()
    return {
        'type': 'class',
        'bases': bases,
        'name': class_name,
        'methods': methods,
        'attrs': attrs,
    }

  def parse_method(self):
    self.expect('def')
    method_name = self.expect('NAME').value
    self.expect('(')
    args = []
    vararg = None
    while not self.consume(')'):
      if self.consume('*'):
        vararg = self.expect('NAME').value
        self.expect(')')
        break
      else:
        args.append(self.expect('NAME').value)
        self.consume(',')
    self.expect('NEWLINE')
    self.expect('INDENT')
    stmts = []
    vars_ = []
    while not self.consume('DEDENT'):
      if self.consume('pass'):
        self.expect('NEWLINE')
      elif self.consume('var'):
        while not self.consume('NEWLINE'):
          vars_.append(self.expect('NAME').value)
          if vars_[-1] in vars_[:-1]:
            raise SyntaxError('Duplicate method var %r' % vars_[-1])
          self.consume(',')
      else:
        stmts.append(self.parse_statement())
    return {
        'type': 'method',
        'name': method_name,
        'args': args,
        'vararg': vararg,
        'vars': vars_,
        'body': {
            'type': 'block',
            'stmts': stmts,
        },
    }

  def parse_statement_block(self):
    self.expect('INDENT')
    stmts = []
    while not self.consume('DEDENT'):
      if self.consume('pass'):
        self.expect('NEWLINE')
      else:
        stmts.append(self.parse_statement())
    return {
        'type': 'block',
        'stmts': stmts,
    }

  def parse_statement(self):
    if self.at('if'):
      return self.parse_if_statement()
    elif self.consume('while'):
      cond = self.parse_expression()
      self.expect('NEWLINE')
      body = self.parse_statement_block()
      return {
          'type': 'while',
          'cond': cond,
          'body': body,
      }
    elif self.consume('break'):
      self.expect('NEWLINE')
      return {'type': 'break'}
    elif self.consume('continue'):
      self.expect('NEWLINE')
      return {'type': 'continue'}
    elif self.consume('return'):
      value = self.parse_expression()
      self.expect('NEWLINE')
      return {
          'type': 'return',
          'value': value,
      }
    else:
      expr = self.parse_expression()
      self.expect('NEWLINE')
      return {
          'type': 'expr',
          'expr': expr,
      }

  def parse_if_statement(self):
    self.expect('if')
    cond = self.parse_expression()
    self.expect('NEWLINE')
    body = self.parse_statement_block()
    if self.consume('else'):
      if self.consume('NEWLINE'):
        otherwise = self.parse_statement_block()
      else:
        otherwise = self.parse_if_statement()
    else:
      otherwise = None
    return {
        'type': 'if',
        'cond': cond,
        'body': body,
        'else': otherwise,
    }

  def parse_expression(self):
    return self.parse_additive_expression()

  def parse_additive_expression(self):
    expr = self.parse_multiplicative_expression()
    while any(self.at(op) for op in ('+', '-')):
      op = self.next().type
      rhs = self.parse_multiplicative_expression()
      expr = {
          'type': 'binop',
          'op': op,
          'lhs': expr,
          'rhs': rhs,
      }
    return expr

  def parse_multiplicative_expression(self):
    expr = self.parse_prefix_expression()
    while any(self.at(op) for op in ('*', '/', '%')):
      op = self.next().type
      rhs = self.parse_prefix_expression()
      expr = {
          'type': 'binop',
          'op': op,
          'lhs': expr,
          'rhs': rhs,
      }
    return expr

  def parse_prefix_expression(self):
    if any(self.at(op) for op in ('-', '+')):
      op = self.next().type
      return {
          'type': 'preop',
          'op': op,
          'expr': self.parse_postfix_expression(),
      }
    return self.parse_postfix_expression()

  def parse_postfix_expression(self):
    expr = self.parse_primary_expression()
    while True:
      if self.consume('.'):
        attr = self.expect('NAME').value
        if self.at('('):
          args, vararg = self.parse_arglist()
          expr = {
              'type': 'call-method',
              'owner': expr,
              'name': attr,
              'args': args,
              'vararg': vararg,
          }
        elif self.consume('='):
          value = self.parse_expression()
          expr = {
              'type': 'setattr',
              'owner': expr,
              'name': attr,
              'value': value,
          }
        else:
          expr = {
              'type': 'getattr',
              'owner': expr,
              'name': attr,
          }
      elif self.consume('['):
        index = self.parse_expression()
        self.expect(']')
        if self.consume('='):
          expr = {
              'type': 'setitem',
              'owner': expr,
              'index': index,
              'value': self.parse_expression(),
          }
        else:
          expr = {
              'type': 'getitem',
              'owner': expr,
              'index': index,
          }
      else:
        break
    return expr

  def parse_primary_expression(self):
    if self.consume('('):
      expr = self.parse_expression()
      self.expect(')')
      return expr
    elif self.at('NUMBER'):
      return {
          'type': 'num',
          'value': self.expect('NUMBER').value,
      }
    elif self.at('NAME'):
      name = self.expect('NAME').value
      if self.at('('):
        args, vararg = self.parse_arglist()
        return {
            'type': 'call-func',
            'name': name,
            'args': args,
            'vararg': vararg,
        }
      elif self.consume('='):
        return {
            'type': 'assign',
            'name': name,
            'value': self.parse_expression(),
        }
      else:
        return {
            'type': 'name',
            'value': name,
        }
    elif self.at('STRING'):
      return {
          'type': 'str',
          'value': self.expect('STRING').value,
      }
    elif self.at('['):
      values = []
      while not self.consume(']'):
        values.append(self.parse_expression())
        self.consume(',')
      return {
          'type': 'list',
          'values': values,
      }
    elif self.at('{'):
      items = []
      while not self.consume('}'):
        key = self.parse_expression()
        if self.consume(':'):
          val = self.parse_expression()
        else:
          val = None
        items.append((key, val))
        self.consume(',')
      return {
          'type': 'dict',
          'items': items,
      }
    elif self.consume('new'):
      class_name = self.next().value
      args, vararg = self.parse_arglist()
      return {
          'type': 'new',
          'name': class_name,
          'args': args,
          'vararg': vararg,
      }

    raise SyntaxError('Expected expression')

  def parse_arglist(self):
    self.expect('(')
    args = []
    vararg = None
    while not self.consume(')'):
      if self.consume('*'):
        vararg = self.parse_expression()
        self.expect(')')
        break
      else:
        args.append(self.parse_expression())
        self.consume(',')
    return args, vararg

expr = Parser().init('<test>', 'x = 3').parse_expression()
assert expr == {
    'type': 'assign',
    'name': 'x',
    'value': {
        'type': 'num',
        'value': 3,
    }
}, expr

expr = Parser().init('<test>', 'x = y.z').parse_expression()
assert expr == {
    'type': 'assign',
    'name': 'x',
    'value': {
        'type': 'getattr',
        'owner': {
            'type': 'name',
            'value': 'y',
        },
        'name': 'z',
    },
}, expr

expr = Parser().init('<test>', 'x + - 3').parse_expression()
assert expr == {
    'type': 'binop',
    'op': '+',
    'lhs': {
        'type': 'name',
        'value': 'x',
    },
    'rhs': {
        'type': 'preop',
        'op': '-',
        'expr': {
          'type': 'num',
          'value': 3,
        },
    },
}, expr

expr = Parser().init('<test>', 'x + -3').parse_expression()
assert expr == {
    'type': 'binop',
    'op': '+',
    'lhs': {
        'type': 'name',
        'value': 'x',
    },
    'rhs': {
      'type': 'num',
      'value': -3,
    },
}, expr

expr = Parser().init('<test>', 'x + -y').parse_expression()
assert expr == {
    'type': 'binop',
    'op': '+',
    'lhs': {
        'type': 'name',
        'value': 'x',
    },
    'rhs': {
        'type': 'preop',
        'op': '-',
        'expr': {
          'type': 'name',
          'value': 'y',
        },
    },
}, expr

expr = Parser().init('<test>', 'new C(a, b)').parse_expression()
assert expr == {
    'type': 'new',
    'name': 'C',
    'args': [
        {
            'type': 'name',
            'value': 'a',
        },
        {
            'type': 'name',
            'value': 'b',
        },
    ],
    'vararg': None,
}, expr

expr = Parser().init('<test>', r"""

def blarg(x, y, *z)
  var a b c

""").parse_method()
assert expr == {
    'type': 'method',
    'name': 'blarg',
    'args': ['x', 'y'],
    'vararg': 'z',
    'vars': ['a', 'b', 'c'],
    'body': {
        'type': 'block',
        'stmts': [],
    },
}, expr

expr = Parser().parse('<test>', r"""

include module_name

def some_func()
  pass
""")
assert expr == {
    'type': 'module',
    'name': '<test>',
    'includes': [
        'module_name',
    ],
    'vars': [],
    'funcs': [
        {
            'type': 'method',
            'name': 'some_func',
            'vars': [],
            'vararg': None,
            'args': [],
            'body': {
                'type': 'block',
                'stmts': [],
            },
        },
    ],
    'stmts': [],
    'classes': [],
}, expr


