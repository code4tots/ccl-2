# python again.py test.ccl *.ccl && javac *.java -d cls && java -cp cls com.ccl.Modules
import sys

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

  def line_number(self):
    return self.source.string.count('\n', 0, self.i) + 1

  def column_number(self):
    return self.i - self.source.string.rfind('\n', 0, self.i)

  def line(self):
    start = self.source.string.rfind('\n', 0, self.i) + 1
    end = self.source.string.find('\n', self.i)
    if end == -1:
      end = len(self.source.string)
    return self.source.string[start:end]

class Err(Exception):
  def __init__(self, token, message):
    super(Err, self).__init__(
        message + '\n on line %d in %s\n%s\n%s' % (
          token.line_number(),
          token.source.filespec,
          token.line(),
          ' ' * (token.column_number()-1) + '*'))
    self.token = token

SYMBOLS = {
  '=', '\\', '\\\\', '.', '$', '/', '*', ',',
  '{', '}', '(', ')', '[', ']'}
KEYWORDS = {
  'return', 'if', 'else', 'while', 'break', 'continue',
  'import', 'and', 'or'}

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
          raise Err(
            Token(source, j, 'ERR'),
            'Unterminated string literal')
        i += 2 if s[i] == '\\' else 1
      i += len(quote)
      toks.append(Token(source, j, 'STRING', eval(s[j:i])))
      continue

    # int and float
    seen_dot = False
    seen_digit = False
    if i < len(s) and s[i] == '-':
      i += 1
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
        toks.append(Token(source, j, 'NAME', name))
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
    raise Err(Token(source, j, 'ERR'), 'Invalid token: ' + s[j:i])

  toks.append(Token(source, i, 'EOF'))
  return toks

pairs = [(token.type, token.value) for token in lex(Source('<test>', """
'hi'
55
6.60
hi
return
=
-5.0
"""))]

assert pairs == [
  ('STRING', 'hi'),
  ('INT', 55),
  ('FLOAT', 6.6),
  ('NAME', 'hi'),
  ('return', None),
  ('=', None),
  ('FLOAT', -5.0),
  ('EOF', None),
], pairs

ln = lex(Source('<test>', "hi\nthere"))[0].line_number()
assert ln == 1, ln

class Ast(object):
  def __init__(self, token, *args):
    attrs = type(self).attrs
    if len(attrs) != len(args):
      raise Err(token, 'Expected %d args, but got %d' % (
        len(attrs), len(args)))
    for name, value in zip(attrs, args):
      setattr(self, name, value)

def tuplify(x):
  if isinstance(x, (str, type(None))):
    return x
  elif isinstance(x, list):
    return [tuplify(y) for y in x]
  elif isinstance(x, Ast):
    return (
      (type(x).__name__,) +
      tuple(tuplify(getattr(x, attr)) for attr in type(x).attrs))
  raise Exception(type(x))

class Module(Ast):
  attrs = ['name', 'body']

class ImportStatement(Ast):
  attrs = ['name']

class IfStatement(Ast):
  attrs = ['cond', 'body', 'other']

class WhileStatement(Ast):
  attrs = ['cond', 'body']

class BreakStatement(Ast):
  attrs = []

class ContinueStatement(Ast):
  attrs = []

class BlockStatement(Ast):
  attrs = ['stmts']

class ReturnStatement(Ast):
  attrs = ['expr']

class ExpressionStatement(Ast):
  attrs = ['expr']

class IntExpression(Ast):
  attrs = ['val']

class FloatExpression(Ast):
  attrs = ['val']

class StringExpression(Ast):
  attrs = ['val']

class NameExpression(Ast):
  attrs = ['name']

class AssignExpression(Ast):
  attrs = ['pattern', 'expr']

class FunctionExpression(Ast):
  attrs = ['scoped', 'pattern', 'body']

class FunctionCallExpression(Ast):
  attrs = ['f', 'args', 'vararg']

class MethodCallExpression(Ast):
  attrs = ['self', 'name', 'args', 'vararg']

class LogicalOrExpression(Ast):
  attrs = ['left', 'right']

class LogicalAndExpression(Ast):
  attrs = ['left', 'right']

class NamePattern(Ast):
  attrs = ['name']

class ListPattern(Ast):
  attrs = ['args', 'opts', 'vararg']

def module_name_from_filespec(filespec):
  name = filespec.split('/')[-1]
  if name.endswith('.ccl'):
    name = name[:-len('.ccl')]
  if not all(c.isalnum() or c == '_' for c in name):
    raise Exception('Invalid module name: ' + name)
  return name

def sanitize_string(s):
  return '"%s"' % s.replace('\\', '\\\\')

class Parser(object):
  def __init__(self, source):
    self.source = source
    self.tokens = lex(source)
    self.i = 0

  def peek(self):
    return self.tokens[self.i]

  def at(self, type_):
    return (
      self.peek().type in type_ if isinstance(type_, tuple) else
      self.peek().type == type_)

  def next(self):
    self.i += 1
    return self.tokens[self.i-1]

  def consume(self, type_):
    if self.at(type_):
      return self.next()

  def expect(self, type_):
    if not self.at(type_):
      raise Err(self.peek(), 'Expected %s' % type_)
    return self.next()

  def parseModule(self):
    token = self.peek()
    stmts = []
    while not self.at('EOF'):
      stmts.append(self.parseStatement())
    return Module(
      token,
      module_name_from_filespec(token.source.filespec),
      BlockStatement(token, stmts))

  def parseStatement(self):
    if self.at('if'):
      token = self.next()
      cond = self.parseExpression()
      body = self.parseStatement()
      other = self.parseStatement() if self.consume('else') else None
      return IfStatement(token, cond, body, other)
    elif self.at('while'):
      token = self.next()
      cond = self.parseExpression()
      body = self.parseStatement()
      return WhileStatement(token, cond, body)
    elif self.at('break'):
      return BreakStatement(self.next())
    elif self.at('continue'):
      return ContinueStatement(self.next())
    elif self.at('{'):
      token = self.next()
      stmts = []
      while not self.consume('}'):
        stmts.append(self.parseStatement())
      return BlockStatement(token, stmts)
    elif self.at('return'):
      token = self.next()
      expr = self.parseExpression()
      return ReturnStatement(token, expr)
    else:
      token = self.peek()
      expr = self.parseExpression()
      return ExpressionStatement(token, expr)

  def parseExpression(self):
    return self.parseLogicalOrExpression()

  def parseLogicalOrExpression(self):
    left = self.parseLogicalAndExpression()
    while self.at('or'):
      token = self.next()
      right = self.parseLogicalAndExpression()
      left = LogicalOrExpression(token, left, right)
    return left

  def parseLogicalAndExpression(self):
    left = self.parsePostfixExpression()
    while self.at('and'):
      token = self.next()
      right = self.parsePostfixExpression()
      left = LogicalAndExpression(token, left, right)
    return left

  def parsePostfixExpression(self):
    expr = self.parsePrimaryExpression()
    while True:
      if self.at('.'):
        token = self.next()
        name = self.expect('NAME').value
        if self.at('['):
          args, vararg = self.parseArguments()
          expr = MethodCallExpression(token, expr, name, args, vararg)
        elif self.at('='):
          val = self.parseExpression()
          expr = SetAttributeExpression(token, expr, name, val)
        else:
          expr = GetAttributeExpression(token, expr, name)
      elif self.at('['):
        token = self.peek()
        args, vararg = self.parseArguments()
        expr = FunctionCallExpression(token, expr, args, vararg)
      else:
        break
    return expr

  def parsePrimaryExpression(self):
    token = self.peek()
    if self.at('INT'):
      return IntExpression(token, self.next().value)
    if self.at('FLOAT'):
      return FloatExpression(token, self.next().value)
    if self.at('STRING'):
      return StringExpression(token, self.next().value)
    if self.at('NAME'):
      name = self.next().value
      if self.consume('='):
        return AssignExpression(
          token, NamePattern(token, name), self.parseExpression())
      else:
        return NameExpression(token, name)
    if self.consume('$'):
      pattern = self.parsePattern()
      self.expect('=')
      expr = self.parseExpression()
      return AssignExpression(pattern, expr)
    if self.at(('\\', '\\\\')):
      scoped = self.next().type == '\\\\'
      pattern = self.parsePatterns(token)
      if self.consume('.'):
        expr = self.parseExpression()
        body = ReturnStatement(token, expr)
      else:
        if self.peek().type != '{':
          raise Err(
            self.peek(),
            "Expected '{' or '.' to indicate end of argument list.")
        body = self.parseStatement()
      return FunctionExpression(token, scoped, pattern, body)
    raise Err(token, "Expected expression")

  def parseArguments(self):
    self.expect('[')
    args = []
    vararg = None
    while not self.consume(']'):
      if self.consume('*'):
        vararg = self.expect('NAME').value
        self.expect(']')
        break
      else:
        args.append(self.parseExpression())
        self.consume(',')
    return args, vararg

  def parsePatterns(self, token):
    args = []
    while self.at(('NAME', '[')):
      args.append(self.parsePattern())
    opts = []
    while self.consume('/'):
      opts.append(self.expect('NAME').value)
    vararg = None
    if self.consume('*'):
      vararg = self.expect('NAME').value
    return ListPattern(token, args, opts, vararg)

  def parsePattern(self):
    if self.at('['):
      return self.parsePattern(self.next())
    else:
      return NamePattern(self.expect('NAME').value)

def parse(source):
  return Parser(source).parseModule()

dat = tuplify(parse(Source('test.ccl', r"""

if a or b {
  print[c]
  d.call[e, *f]
}

""")))

assert dat == ('Module', 'test', ('BlockStatement', [('IfStatement', ('LogicalOrExpression', ('NameExpression', 'a'), ('NameExpression', 'b')), ('BlockStatement', [('ExpressionStatement', ('FunctionCallExpression', ('NameExpression', 'print'), [('NameExpression', 'c')], None)), ('ExpressionStatement', ('MethodCallExpression', ('NameExpression', 'd'), 'call', [('NameExpression', 'e')], 'f'))]), None)])), dat

class Translator(object):

  def translate(self, modules):
    return r"""package com.ccl;

public class Modules extends Runtime {

  public static void main(String[] args) {
    import_%s();
  }%s
}
""" % (
    modules[0].name,
    ''.join(self.visit(m) for m in modules).replace('\n', '\n  '))

  def visit(self, node):
    return getattr(self, 'visit' + type(node).__name__)(node)

  def visitModule(self, node):
    return r"""

private static Scope module_{name} = null;
public static Value import_{name}() {{
  if (module_{name} == null) {{
    module_{name} = new Scope(GLOBAL);
    run_{name}(module_{name});
  }}
  return module_{name};
}}
private static void run_{name}(Scope scope) {{
  Value condition;{body}
}}""".format(name=node.name, body=self.visit(node.body).replace('\n', '\n  '))

  def visitBlockStatement(self, node):
    if node.stmts:
      return '\n{%s\n}' % ''.join(
        self.visit(s) for s in node.stmts).replace('\n', '\n  ')
    else:
      return ''

  def visitExpressionStatement(self, node):
    return '\n' + self.visit(node.expr) + ';'

  def visitFunctionCallExpression(self, node):
    vararg = 'null' if node.vararg is None else self.visit(node.vararg)
    return '%s.call(nil, new Value[]{%s}, %s)' % (
      self.visit(node.f),
      ', '.join(self.visit(a) for a in node.args),
      vararg)

  def visitStringExpression(self, node):
    return 'Str.from(' + sanitize_string(node.val) + ')'

  def visitNameExpression(self, node):
    return 'scope.getvar("%s")' % node.name

def translate(modules):
  return Translator().translate(modules)

def translate_files(filepaths):
  modules = []
  seen = set()
  for filepath in filepaths:
    if filepath in seen:
      continue
    seen.add(filepath)
    with open(filepath) as f:
      contents = f.read()
    modules.append(parse(Source(filepath, contents)))
  return translate(modules)

if __name__ == '__main__':
  if len(sys.argv) < 2:
    print('usage: %s <main_module> <library_modules...>' % sys.argv[0])
    exit(1)
  contents = translate_files(sys.argv[1:])
  with open('Modules.java', 'w') as f:
    f.write(contents)

