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
  def __init__(self):
    self.globals = set()
    self.locals = None

  def contains(self, name):
    return (
        self.locals is not None and name in self.locals or
        name in self.globals)

  def assign(self, name):
    if self.locals is not None:
      self.locals.add(name)
    else:
      self.globals.add(name)

  def visitModule(self, node):
    self.visit(node.block)
    node.vars = set(self.globals)

  def visitBlockStatement(self, node):
    for stmt in node.stmts:
      self.visit(stmt)

  def visitExpressionStatement(self, node):
    self.visit(node.expr)

  def visitNameExpression(self, node):
    if not self.contains(node.name):
      raise common.TranslationError(
          node.token, 'Variable used before assigned')

  def visitAssignExpression(self, node):
    self.assign(node.name)
    self.visit(node.expr)

  def visitMethodCallExpression(self, node):
    self.visit(node.expr)
    for arg in node.args:
      self.visit(arg)

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
a = Annotator()
a.visit(m)
assert m.vars == set(['x', 'y']), m.vars
