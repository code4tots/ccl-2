"""

get[ls List[?], i Int] {
  ...
}

f[a, b] {
  return add[a, b]
}

Main[args List[String]] {
  f[get[args, 0], get[args, 1]]
}

"""

import common

class Ast(common.Ast):
  pass

class Type(Ast):
  pass

class Statement(Ast):
  pass

class Expression(Ast):
  pass

class FunctionDefinition(Ast):
  attrs = [
      ('name', str),
      ('args', [(str, Type)]),
      ('body', [Statement]),
  ]

class Module(Ast):
  attrs = [
      ('funcs', [FunctionDefinition]),
  ]

class NamedType(Type):
  attrs = [('name', str)]

class BlankType(Type):
  attrs = []

class ParametricType(Type):
  attrs = [('name', str), ('args', [Type])]

class ExpressionStatement(Statement):
  attrs = [('expr', Expression)]

class CallExpression(Expression):
  attrs = [('f', str), ('args', [Expression])]

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
