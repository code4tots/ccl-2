"""

# Naming convention:
# types and function names are CamelCase,
# everything else is snake_case.

struct List[T] {
  len Int
  cap Int
  buf Array[T]
}

Get[ls List[?T], i Int] {
  return Get[ls.buf, i]
}

MakeList[a Array[?T]] {
  let len = Size[a]
  let cap = Add[Mul[2, len], 10]
  let buf = MakeArray[cp]
  let i = 0
  while Lt[i, len] {
    Set[buf, i, Get[a, i]]
  }

  # The type parameters to 'List' should be deducible from
  # its constructor arguments (i.e. its members).
  return List[len, cap, buf]

  # # Or
  # let list = new List[T]
  # list.len = len
  # list.cap = cap
  # list.buf = buf
  # return list
  #
  # # Values created using the 'new' keyword initializes to default values.
  # # Builtin types have specified default values (e.g. Int -> 0),
  # # and aggregate types (i.e. 'struct') default to setting all its members
  # # to their corresponding default values.
}

F[a ?_, b ?_] {
  return Add[a, b]
}

Main[args Array[String]] {
  F[Get[args, 0], Get[args, 1]]
  Print[ %[1, 2, 3] ]
  Print[ MakeList[%[1, 2, 3]] ]
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
