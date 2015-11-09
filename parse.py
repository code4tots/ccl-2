# TODO: Clean up this crap too.
# By clean up, I mean, think through the design more.
from lex import Lexer

BINOPS = {
  '*': '__mul__',
  '/': '__div__',
  '%': '__mod__',
  '+': '__add__',
  '-': '__sub__',
  '<': '__lt__',
  '<=': '__le__',
  '>': '__gt__',
  '>=': '__ge__',
  '==': '__eq__',
  '!=': '__ne__',

  # TODO: Figure out a nice way to integrate these ops
  # with the split system I already have of
  # '__getitem__/__setitem__', variables, and attributes.
  '+=': '__iadd__',
  '-=': '__isub__',
  '*=': '__imul__',
  '/=': '__idiv__',
  '%=': '__imod__',
}

class Parser(object):

  ## public

  def parse(self, filespec, string):
    self.init(filespec, string)
    return self.parse_module()

  ## private

  def init(self, filespec, string):
    self.tokens = Lexer().lex(filespec, string)
    self.position = 0
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

  def parse_module(self):
    includes = []
    classes = []
    self.skip_newlines()
    while not self.at('EOF'):
      if self.at('include'):
        includes.append(self.parse_include())
      elif self.at('class'):
        classes.append(self.parse_class())
      else:
        # TODO: Better error message.
        raise SyntaxError(
            'Expected class or include but found %r' % self.peek())
      self.skip_newlines()

    return {
        'type': 'module',
        'includes': includes,
        'classes': classes,
    }

  def parse_include(self):
    self.expect('include')
    name = self.expect('NAME').value
    self.expect('NEWLINE')
    return {
        'type': 'include',
        'name': name,
    }

  def parse_class(self):
    self.expect('class')
    name = self.expect('NAME').value
    bases = []
    attrs = []
    methods = []

    if self.consume(':'):
      while not self.at('NEWLINE'):
        bases.append(self.expect('NAME').value)

    self.expect('NEWLINE')
    self.expect('INDENT')

    while not self.consume('DEDENT'):
      if self.consume('pass'):
        self.expect('NEWLINE')
      elif self.consume('var'):
        while not self.consume('NEWLINE'):
          attrs.append(self.expect('NAME').value)
      else:
        methods.append(self.parse_method())

    return {
        'type': 'class',
        'name': name,
        'bases': bases,
        'attrs': attrs,
        'methods': methods,
    }

  def parse_method(self):
    self.expect('def')
    name = self.expect('NAME').value
    args = []
    self.expect('(')
    while not self.consume(')'):
      args.append(self.expect('NAME').value)
      self.consume(',')
    self.expect('NEWLINE')
    body = self.parse_statement_block()
    return {
        'type': 'method',
        'name': name,
        'args': args,
        'body': body,
    }

  # TODO: Right now parse_statement* methods
  # return C source as strings because I'm lazy.
  # In the future though, I might want to actually
  # return some sort of tree so that I can do other fun
  # things with it.

  def parse_statement_block(self):
    stmts = []
    self.expect('INDENT')
    self.skip_newlines()
    while not self.consume('DEDENT'):
      if self.consume('pass'):
        self.expect('NEWLINE')
      else:
        stmts.append(self.parse_statement())
      self.skip_newlines()
    return '\n{%s\n}' % ''.join(stmt.replace('\n', '\n  ') for stmt in stmts)

  def parse_statement(self):
    if self.consume('return'):
      value = self.parse_expression()
      self.expect('NEWLINE')
      return '\nreturn %s;' % value
    elif self.consume('break'):
      self.expect('NEWLINE')
      return '\nbreak;'
    elif self.consume('var'):
      names = []
      while not self.consume('NEWLINE'):
        names.append(self.expect('NAME').value)
      return '\nCCL_Object %s;' % ', '.join('*CCL_var_' + n for n in names)
    else:
      value = self.parse_expression()
      self.expect('NEWLINE')
      return '\n%s;' % value

  # TODO: Some of the operations below are sensitive to
  # C's precedence rules. In cleaning up, figure out whether
  # this should be a concern.

  def parse_expression(self):
    return self.parse_assign_expression()

  def parse_assign_expression(self):
    expr = self.parse_tenop_expression()
    if self.consume('='):
      rhs = self.parse_assign_expression()
      expr = '%s = %s' % (expr, rhs)
    return expr

  def parse_tenop_expression(self):
    expr = self.parse_or_expression()
    if self.consume('?'):
      lhs = self.parse_expression()
      self.expect(':')
      rhs = self.parse_tenop_expression()
      expr = 'CCL_truthy(%s) ? %s : %s' % (expr, lhs, rhs)
    return expr

  def parse_or_expression(self):
    parse_subexpr = self.parse_and_expression
    expr = parse_subexpr()
    while self.consume('or'):
      expr = '(CCL_truthy(%s) || CCL_truthy(%s) ? CCL_true : CCL_false)' % (expr, parse_subexpr())
    return expr    

  def parse_and_expression(self):
    parse_subexpr = self.parse_relational_expression
    expr = parse_subexpr()
    while self.consume('and'):
      expr = '(CCL_truthy(%s) && CCL_truthy(%s) ? CCL_true : CCL_false)' % (expr, parse_subexpr())
    return expr

  def parse_relational_expression(self):
    expr = self.parse_additive_expression()
    for op in ('<', '<=', '>', '>=', '==', '!='):
      if self.consume(op):
        expr = 'CCL_invoke_method(%s, "%s", 1, %s)' % (
            expr, BINOPS[op], self.parse_relational_expression())
        break
    else:
      if self.consume('in'):
        cont = self.parse_relational_expression()
        expr = 'CCL_invoke_method(%s, "__contains__", 1, %s)' % (cont, expr)
    return expr

  def parse_additive_expression(self):
    parse_subexpr = self.parse_multiplicative_expression
    expr = parse_subexpr()
    while True:
      for op in ('+', '-'):
        if self.consume(op):
          expr = 'CCL_invoke_method(%s, "%s", 1, %s)' % (
              expr, BINOPS[op], parse_subexpr())
          break
      else:
        break
    return expr

  def parse_multiplicative_expression(self):
    parse_subexpr = self.parse_prefix_expression
    expr = parse_subexpr()
    while True:
      for op in ('*', '/', '%'):
        if self.consume(op):
          expr = 'CCL_invoke_method(%s, "%s", 1, %s)' % (
              expr, BINOPS[op], parse_subexpr())
          break
      else:
        break
    return expr

  def parse_prefix_expression(self):
    if self.consume('-'):
      return 'CCL_invoke_method(%s, "__neg__", 0)' % (
          self.parse_postfix_expression())
    if self.consume('+'):
      return 'CCL_invoke_method(%s, "__pos__", 0)' % (
          self.parse_postfix_expression())
    if self.consume('not'):
      return 'CCL_truthy(%s) ? CCL_false : CCL_true' % (
          self.parse_postfix_expression())
    return self.parse_postfix_expression()

  def parse_postfix_expression(self):
    expr = self.parse_primary_expression()
    while True:
      if self.consume('.'):
        attr = self.expect('NAME').value
        if self.consume('('):
          args = []
          while not self.consume(')'):
            args.append(self.parse_expression())
            self.consume(',')
          expr = 'CCL_invoke_method(%s, "%s", %d, %s)' % (
              expr, attr, len(args), ', '.join(args))
        elif self.consume('='):
          val = self.parse_expression()
          expr = 'CCL_set_attribute(%s, "%s", %s)' % (expr, attr, val)
        else:
          expr = 'CCL_get_attribute(%s, "%s")' % (expr, attr)
      elif self.consume('['):
        arg = self.parse_expression()
        self.expect(']')
        if self.consume('='):
          val = self.parse_expression()
          expr = 'CCL_invoke_method(%s, "__setitem__", 2, %s, %s)' % (
              expr, arg, val)
        else:
          expr = 'CCL_invoke_method(%s, "__getitem__", 1, %s)' % (expr, arg)
      else:
        break
    return expr

  def parse_primary_expression(self):
    tok = self.consume('NAME')
    if tok:
      return 'CCL_var_' + tok.value

    tok = self.consume('NUMBER')
    if tok:
      return 'CCL_new_Num(%s)' % tok.value

    tok = self.consume('STRING')
    if tok:
      return 'CCL_new_Str("%s")' % (tok.value
          .replace('\n', '\\n')
          .replace('\t', '\\t')
          .replace('"', '\\"'))

    if self.consume('['):
      items = []
      while not self.consume(']'):
        items.append(self.parse_expression())
        self.consume(',')
      return 'CCL_new_List(%d, %s)' % (len(items), ', '.join(items))

    if self.consume('{'):
      items = []
      while not self.consume('}'):
        items.append(self.parse_expression())
        if self.consume(':'):
          items.append(self.parse_expression())
        else:
          items.append('CCL_true')
        self.consume(',')
      return 'CCL_new_Dict(%d, %s)' % (len(items), ', '.join(items))

    if self.consume('('):
      expr = self.parse_expression()
      self.expect(')')
      return '(%s)' % expr

    if self.consume('nil'):
      return 'CCL_nil'

    if self.consume('true'):
      return 'CCL_true'

    if self.consume('false'):
      return 'CCL_false'

    if self.consume('self'):
      return 'CCL_self'

    # TODO: Better error message
    raise SyntaxError('Expected expression')

### Tests

parsed_include = Parser().init('<test>', r"""
include blarglib
""").parse_include()
assert parsed_include == {
  'type': 'include',
  'name': 'blarglib',
}, parsed_include

parsed_class = Parser().init('<test>', r"""
class Blarg
  pass
""").parse_class()
assert parsed_class == {
  'type': 'class',
  'name': 'Blarg',
  'bases': [],
  'attrs': [],
  'methods': [],
}, parsed_class

parsed_class = Parser().init('<test>', r"""
class Blarg: Base
  var a b c
""").parse_class()
assert parsed_class == {
  'type': 'class',
  'name': 'Blarg',
  'bases': ['Base'],
  'attrs': ['a', 'b', 'c'],
  'methods': [],
}, parsed_class

parsed_class = Parser().init('<test>', r"""
class Blarg: Base
  var a b c

  def sample_method(x, y, z)
    pass
""").parse_class()
assert parsed_class == {
  'type': 'class',
  'name': 'Blarg',
  'bases': ['Base'],
  'attrs': ['a', 'b', 'c'],
  'methods': [
      {
          'type': 'method',
          'name': 'sample_method',
          'args': ['x', 'y', 'z'],
          'body': '\n{\n}',
      }
  ],
}, parsed_class

parsed_class = Parser().init('<test>', r"""
class Blarg: Base
  var a b c

  def sample_method(x, y, z)
    var d
    return x
""").parse_class()
assert parsed_class == {
  'type': 'class',
  'name': 'Blarg',
  'bases': ['Base'],
  'attrs': ['a', 'b', 'c'],
  'methods': [
      {
          'type': 'method',
          'name': 'sample_method',
          'args': ['x', 'y', 'z'],
          'body': """
{
  CCL_Object *CCL_var_d;
  return CCL_var_x;
}""",
      }
  ],
}, parsed_class

parsed_expr = Parser().init('<test>', r"""
hi
""").parse_expression()
assert parsed_expr == 'CCL_var_hi', parsed_expr

parsed_expr = Parser().init('<test>', r"""
a.b(c)
""").parse_expression()
assert (
    parsed_expr == 'CCL_invoke_method(CCL_var_a, "b", 1, CCL_var_c)'
), parsed_expr

parsed_expr = Parser().init('<test>', r"""
a[5]
""").parse_expression()
assert (
    parsed_expr == 'CCL_invoke_method(CCL_var_a, "__getitem__", 1, CCL_new_Num(5.0))'
), parsed_expr

parsed_expr = Parser().init('<test>', r"""
a * b * c
""").parse_expression()
assert (
    parsed_expr == 'CCL_invoke_method(CCL_invoke_method(CCL_var_a, "__mul__", 1, CCL_var_b), "__mul__", 1, CCL_var_c)'
), parsed_expr

parsed_expr = Parser().init('<test>', r"""
a = b - c
""").parse_expression()
assert (
    parsed_expr == 'CCL_var_a = CCL_invoke_method(CCL_var_b, "__sub__", 1, CCL_var_c)'
), parsed_expr

parsed_expr = Parser().init('<test>', r"""
a or b
""").parse_expression()
assert (
    parsed_expr == '(CCL_truthy(CCL_var_a) || CCL_truthy(CCL_var_b) ? CCL_true : CCL_false)'
), parsed_expr

parsed_expr = Parser().init('<test>', r"""
a and not b
""").parse_expression()
assert (
    parsed_expr == '(CCL_truthy(CCL_var_a) && CCL_truthy(CCL_truthy(CCL_var_b) ? CCL_false : CCL_true) ? CCL_true : CCL_false)'
), parsed_expr
