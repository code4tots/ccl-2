import common

class Ast(common.Ast):
  pass

class Statement(Ast):
  pass

class Expression(Ast):
  pass

class BlockStatement(Statement):
  attrs = [('stmts', [Statement])]

class ExpressionStatement(Statement):
  attrs = [('expr', Expression)]

class NameExpression(Expression):
  attrs = [('name', str)]

class NumberExpression(Expression):
  attrs = [('val', float)]

class StrExpression(Expression):
  attrs = [('val', str)]

class AssignExpression(Expression):
  attrs = [('name', str), ('expr', Expression)]

class MethodCallExpression(Expression):
  attrs = [('expr', Expression), ('name', str), ('args', [Expression])]

class Module(Ast):
  attrs = [('block', BlockStatement)]

class Annotator(common.AstVisitor):
  def __init__(self, methodsTable=None):
    # In order to annotate expressions with their types, we need
    #  1: Types of variables (local, members, and globals) and
    #  2: Return types of methods.
    # We store (1) in the following three instance members, and (2)
    # in the next one.
    self.globals = dict()
    self.members = None # instance scope variables
    self.locals = None # local scope variables
    self.methods = methodsTable or dict()

  def lookupVariableType(self, name):
    if self.locals and name in self.locals:
      return self.locals[name]
    elif self.members and name in self.members:
      return self.members[name]
    elif self.globals and name in self.globals:
      return self.globals[name]

  def lookupMethodType(self, ownerType, methodName):
    return self.methods.get((ownerType, methodName)) or (None, None)

  def declare(self, token, name, type_):
    d = (
        self.locals if self.locals is not None else
        self.members if self.members is not None else
        self.globals)
    if name in d and d[name] != type_:
      raise common.TranslationError(
          token, 'Variable %s has type %s, '
          'but tried to assign value of type %s' % (name, d[name], type_))
    d[name] = type_

  def visitModule(self, node):
    self.visit(node.block)

  def visitBlockStatement(self, node):
    for stmt in node.stmts:
      self.visit(stmt)

  def visitExpressionStatement(self, node):
    self.visit(node.expr)

  def visitNameExpression(self, node):
    t = self.lookupVariableType(node.name)
    if t is None:
      raise common.TranslationError(
          node.token, 'Variable used before assigned')
    node.type = t
    return t

  def visitAssignExpression(self, node):
    t = self.visit(node.expr)
    self.declare(node.token, node.name, t)
    node.type = t
    return t

  def visitMethodCallExpression(self, node):
    ownerType = self.visit(node.expr)
    args = [self.visit(arg) for arg in node.args]
    t, ts = self.lookupMethodType(ownerType, node.name)
    if t is None:
      raise common.TranslationError(
          node.token, 'Type "%s" does not have method named "%s"' %
          (ownerType, node.name))
    if len(args) != len(ts):
      raise common.TranslationError(
          node.token, 'Method "%s.%s" expected %d arguments but found %d' %
          (ownerType, node.name, len(ts), len(args)))
    for i, (a, ta) in enumerate(zip(args, ts)):
      if a != ta:
        raise common.TranslationError(
            node.args[i].token,
            'Expected argument %d of method "%s.%s" to be '
            '"%s" but found "%s"' %
            (i, ownerType, node.name, ta, a))
    node.type = t
    return t

  def visitNumberExpression(self, node):
    node.type = 'num'
    return node.type

  def visitStrExpression(self, node):
    node.type = 'str'
    return node.type

class Parser(common.Parser):

  def parseModule(self):
    token = self.peek()
    stmts = []
    while not self.at('EOF'):
      stmts.append(self.parseStatement())
    return Module(token, BlockStatement(token, stmts))

  def parseStatement(self):
    expr = self.parseExpression()
    return ExpressionStatement(expr.token, expr)

  def parseExpression(self):
    return self.parsePostfixExpression()

  def parsePostfixExpression(self):
    expr = self.parsePrimaryExpression()
    while True:
      if self.at('.'):
        token = self.next()
        name = self.expect('ID').value
        args = self.parseArguments()
        expr = MethodCallExpression(token, expr, name, args)
      else:
        break
    return expr

  def parseArguments(self):
    self.expect('[')
    args = []
    while not self.consume(']'):
      args.append(self.parseExpression())
      self.consume(',')
    return args

  def parsePrimaryExpression(self):
    if self.at('ID'):
      token = self.next()
      name = token.value
      if self.at('='):
        token = self.next()
        value = self.parseExpression()
        return AssignExpression(token, name, value)
      else:
        return NameExpression(token, name)
    elif self.at('INT') or self.at('FLOAT'):
      token = self.next()
      value = token.value
      return NumberExpression(token, float(value))
    elif self.at('STR'):
      token = self.next()
      value = token.value
      return StrExpression(token, value)

    raise common.ParseError(self.peek(), "Expected expression")

def newMethodTable():
  return {
    ('num', 'add'): ('num', ['num']),
    ('num', 'sub'): ('num', ['num']),
    ('num', 'mul'): ('num', ['num']),
    ('num', 'div'): ('num', ['num']),
    ('num', 'mod'): ('num', ['num']),
  }

### Tests

stmt = Parser('hi', '<test>').parseStatement()
assert str(stmt) == "ExpressionStatement(NameExpression('hi'))", stmt

expr = Parser('"hi"', '<test>').parseExpression()
assert str(expr) == "StrExpression('hi')", expr

Annotator().visit(expr)
assert expr.type == 'str', expr.type

m = Parser(r"""
x = 5
y = x.add[3.0]
""", '<test>').parseModule()
a = Annotator(newMethodTable())
a.visit(m)
assert a.lookupVariableType('x') == 'num', a.lookupVariableType('x')
assert a.lookupVariableType('y') == 'num', a.lookupVariableType('y')
