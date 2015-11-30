import os
import os.path
import sys

def main():
  if len(sys.argv) < 3:
    print 'Usage: python %s javasrcdir/ fn1.ccl fn2.ccl...'
    exit(1)

  outdir = sys.argv[1]

  try:
    os.mkdir(outdir)
  except OSError:
    pass

  for fn in sys.argv[2:]:
    with open(fn) as f:
      c = f.read()
    node = Parser(c, fn).parse()
    out = to_java(node)
    bn = basename(fn)
    outpath = os.path.join(outdir, 'CclModule' + bn.replace('.ccl', '.java'))
    with open(outpath, 'w') as f:
      f.write(out)

def basename(spec):
  if '/' in spec:
    return spec.split('/')[-1]
  if '\\' in spec:
    return spec.split('\\')[-1]
  return spec

### Ast

class AstVisitor(object):

  def visit(self, node):
    method_name = 'visit' + type(node).__name__
    if hasattr(self, method_name):
      return getattr(self, method_name)(node)
    else:
      return self.generic_visit(node)

  def generic_visit(self, node):
    self.visit_children(node)

  def visit_children(self, node):
    for attr in type(node).attrs:
      child = getattr(node, attr)
      if isinstance(child, Ast):
        self.visit(child)
      elif isinstance(child, list):
        for c in child:
          if isinstance(child, Ast):
            self.visit(child)

def token_reference(token):
  return 'LEXER.tokens[%d]' % token.index

def to_java(ast):
  if ast is None:
    return 'null'
  if isinstance(ast, ModuleAst):
    lexer = ast.token.lexer
    if lexer.filespec is None:
      filespec = '<unknown>'
      class_name = 'Ccl'
    else:
      filespec = lexer.filespec
      class_name = basename(filespec)
      if class_name.endswith('.ccl'):
        class_name = class_name[:-len('.ccl')]
    return (
        'public class CclModule%s extends Easy {\n'
        '  private static Value VALUE = null;\n'
        '  public static Lexer LEXER = new Lexer(%s, %s%s);\n'
        '  public static ModuleAst MODULE = new ModuleAst(%s, %s);\n'
        '  public static void importModule(Context c) {\n'
        '    if (VALUE == null) {\n'
        '      runAndGetValue(c, MODULE);\n'
        '      if (c.exc)\n'
        '        return;\n'
        '      VALUE = c.value;\n'
        '    }\n'
        '    c.value = VALUE;\n'
        '  }\n'
        '  public static void main(String[] args) {\n'
        '    run(MODULE);\n'
        '  }\n'
        '}\n') % (
            class_name,
            sanitize_string_for_java(lexer.string),
            sanitize_string_for_java(filespec or (class_name + '.ccl')),
            ''.join(', ' + str(t.i) for t in lexer.tokens),
            token_reference(ast.token), to_java(ast.expr))
  elif isinstance(ast, FuncAst):
    return 'new FuncAst(%s, %s, new String[]{%s}, %s, %s)' % (
        token_reference(ast.token),
        sanitize_string_for_java(ast.name),
        ', '.join(map(to_java, ast.args)),
        to_java(ast.vararg),
        to_java(ast.body))
  elif isinstance(ast, Ast):
    attrs = type(ast).attrs
    return 'new %s(%s%s)' % (
        type(ast).__name__,
        token_reference(ast.token),
        ''.join(', ' + to_java(getattr(ast, a)) for a in attrs))
  elif isinstance(ast, str):
    return sanitize_string_for_java(ast)
  elif isinstance(ast, float):
    return str(ast)
  elif isinstance(ast, list):
    return 'new Ast[]{%s}' % ', '.join(to_java(a) for a in ast)
  else:
    raise Exception(ast)

def sanitize_string_for_java(s):
  ns = '"'
  for c in s:
    if c == '\n':
      ns += '\\n'
    elif c == '"':
      ns += '\\"'
    else:
      ns += c
  ns += '"'
  return ns

class Ast(object):
  def __init__(self, token, *vals):
    cls = type(self)
    if len(cls.attrs) != len(vals):
      raise Exception(
          'Expected %d attrs for %s (%s) but got %d values' % (
              len(cls.attrs), cls.__name__, ' '.join(cls.attrs), len(vals)))
    self.token = token # Token
    for attr, val in zip(cls.attrs, vals):
      setattr(self, attr, val)

class StringAst(Ast):
  attrs = (
      'value', # str
  )

class NumberAst(Ast):
  attrs = (
      'value', # float
  )

class NameAst(Ast):
  attrs = (
      'name', # str
  )

class AssignAst(Ast):
  attrs = (
      'name', # str
      'expr', # Ast
  )

class CallAst(Ast):
  attrs = (
      'f', # Ast
      'args', # [Ast]
      'vararg', # Ast?
  )

class GetAttrAst(Ast):
  attrs = (
      'expr', # Ast
      'attr', # str
  )

class SetAttrAst(Ast):
  attrs = (
      'expr', # Ast
      'attr', # str
      'val', # Ast
  )

class FuncAst(Ast):
  attrs = (
      'name', # str?
      'args', # [str]
      'vararg', # str?
      'body', # Ast
  )

class ClassAst(Ast):
  attrs = (
    'name', # str?
    'bases', # [Ast]
    'varbase', # Ast?
    'body', # Ast
  )

class ReturnAst(Ast):
  attrs = (
    'expr', # Ast?
  )

class BreakAst(Ast):
  attrs = ()

class ContinueAst(Ast):
  attrs = ()

class WhileAst(Ast):
  attrs = (
    'cond', # Ast
    'body', # Ast
  )

class IfAst(Ast):
  attrs = (
    'cond', # Ast
    'body', # Ast
    'other', # Ast?
  )

class BlockAst(Ast):
  attrs = (
    'exprs', # [Ast]
  )

class NotAst(Ast):
  attrs = (
    'expr', # Ast
  )

class OrAst(Ast):
  attrs = (
    'left', # Ast
    'right', # Ast
  )

class AndAst(Ast):
  attrs = (
    'left', # Ast
    'right', # Ast
  )

class ModuleAst(Ast):
  attrs = (
    'expr', # BlockAst
  )

class IsAst(Ast):
  attrs = (
    'left', # Ast
    'right', # Ast
  )

class ImportAst(Ast):
  attrs = (
    'name', # str
  )

### Lexer

SYMBOLS = tuple(reversed(sorted((
    '(', ')', '[', ']', '{', '}',
    '+', '-', '*', '/', '%', '\\', '.', ',', '=',
    '==', '<', '>', '<=', '>=',
))))

KEYWORDS = tuple(reversed(sorted((
    'def', 'class', 'while', 'break', 'continue', 'for', 'in',
    'not', 'is', 'return', 'and', 'or', 'if', 'else', 'import',
))))

class Token(object):
  def __init__(self, lexer, index, i, type_, value=None):
    self.lexer = lexer
    self.index = index
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

# I think it is worth noting that strictly speaking,
# this Lexer class does more than just lex.
# Because I'm lazy, this is also the data structure
# that holds together all the tokens once I am done
# with the lex.
# If this approach becomes unwieldy, it may be worth revisiting
# and breaking up this class.
class Lexer(object):
  def __init__(self, string, filespec=None):
    self.string = string
    self.filespec = filespec
    self.done = False
    self._i = 0
    self._index = 0
    self.tokens = []
    self.peek = None
    self.next()

  def lex(self):
    tokens = []
    while not self.done:
      tokens.append(self.next())
    return tokens

  def next(self):
    token = self.peek
    self.peek = self._next()
    self.tokens.append(self.peek)
    return token

  def _next_index(self):
    i = self._index
    self._index += 1
    return i

  def make_token(self, *args, **kwargs):
    return Token(self, self._next_index(), *args, **kwargs)

  def _next(self):
    self.skip_spaces_and_comments()

    # EOF
    if not self._c:
      self.done = True
      return self.make_token(self._i, 'EOF')

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
      return self.make_token(j, 'STR', eval(self._p(j)))

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

      return self.make_token(j, 'NUM', float(self._p(j)))

    else:
      self._i = j

    # ID and KEYWORDS
    while iswordchar(self._c):
      self._i += 1

    if j != self._i:
      word = self._p(j)
      if word in KEYWORDS:
        return self.make_token(j, word)
      else:
        return self.make_token(j, 'ID', word)

    # SYMBOLS
    if self._s(SYMBOLS):
      sym = max(sym for sym in SYMBOLS if self._s(sym))
      self._i += len(sym)
      return self.make_token(j, sym)

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

  def parse(self):
    token = self._peek
    exprs = []
    while not self._at('EOF'):
      exprs.append(self._expression())
    return ModuleAst(token, BlockAst(token, exprs))

  def _expression(self):
    return self._or_expression()

  def _or_expression(self):
    expr = self._and_expression()
    while True:
      if self._at('or'):
        token = self._next()
        rhs = self._and_expression()
        expr = OrAst(token, expr, rhs)
        continue
      break
    return expr

  def _and_expression(self):
    expr = self._compare_expression()
    while True:
      if self._at('and'):
        token = self._next()
        rhs = self._compare_expression()
        expr = AndAst(token, expr, rhs)
        continue
      break
    return expr

  def _compare_expression(self):
    expr = self._add_expression()
    while True:
      if self._at('=='):
        token = self._next()
        rhs = self._add_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__eq__'),
            [rhs], None)
        continue
      if self._at('!='):
        token = self._next()
        rhs = self._add_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__ne__'),
            [rhs], None)
        continue
      if self._at('<'):
        token = self._next()
        rhs = self._add_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__lt__'),
            [rhs], None)
        continue
      if self._at('<='):
        token = self._next()
        rhs = self._add_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__le__'),
            [rhs], None)
        continue
      if self._at('>'):
        token = self._next()
        rhs = self._add_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__gt__'),
            [rhs], None)
        continue
      if self._at('>='):
        token = self._next()
        rhs = self._add_expression()
        expr = CallAst(
            token,
            GetAttrAst(token, expr, '__ge__'),
            [rhs], None)
        continue
      if self._at('is'):
        token = self._next()
        if self._consume('not'):
          expr = IsNotAst(token, expr, self._add_expression())
        else:
          expr = IsAst(token, expr, self._add_expression())
        continue
      break
    return expr

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

    if self._at('not'):
      token = self._next()
      expr = self._prefix_expression()
      return NotAst(token, expr)

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

    if self._at('import'):
      token = self._next()
      return ImportAst(token, self._expect('ID').value)

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

    if self._at('class'):
      token = self._next()
      name = None
      if self._at('ID'):
        name = self._next().value
      bases = []
      varbase = None
      if self._consume('['):
        bases, varbase = self._argument_list()
        self._expect(']')
      body = self._expression()
      return ClassAst(token, name, bases, varbase, body)

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

if __name__ == '__main__':
  main()
