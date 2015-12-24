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

class TypePattern(Ast):
  pass

class Statement(Ast):
  pass

class Expression(Ast):
  pass

class StructDefinition(Ast):
  attrs = [
      ('name', str),
      ('args', [str]),
      ('attrs', [(str, Type)]),
  ]

class Declaration(Statement):
  attrs = [('name', str), ('expr', Expression)]

class FunctionDefinition(Ast):
  attrs = [
      ('name', str),
      ('args', [(str, TypePattern)]),
      ('type', Type), # return type
      ('body', Statement),
  ]

class Module(Ast):
  attrs = [
      ('decls', [Declaration]),
      ('structs', [StructDefinition]),
      ('funcs', [FunctionDefinition]),
  ]

class ParametricTypePattern(TypePattern):
  attrs = [('name', str), ('args', [TypePattern])]

class VariableTypePattern(TypePattern):
  attrs = [('name', str)]

class ParametricType(Type):
  attrs = [('name', str), ('args', [Type])]

class ExpressionStatement(Statement):
  attrs = [('expr', Expression)]

class IfStatement(Statement):
  attrs = [('cond', Expression), ('body', Statement), ('other', Statement)]

class WhileStatement(Statement):
  attrs = [('cond', Expression), ('body', Statement)]

class BlockStatement(Statement):
  attrs = [('stmts', [Statement])]

class AssignExpression(Expression):
  attrs = [('name', str), ('expr', Expression)]

class CallExpression(Expression):
  attrs = [('f', str), ('args', [Expression])]

class NameExpression(Expression):
  attrs = [('name', str)]

class StrExpression(Expression):
  attrs = [('val', str)]

class IntExpression(Expression):
  attrs = [('val', int)]

class FloatExpression(Expression):
  attrs = [('val', float)]

class Parser(common.Parser):

  def parseModule(self):
    token = self.peek()
    stmts = []
    while not self.at('EOF'):
      if self.at('let'):
        decls.append(self.parseDeclaration())
      elif self.at('struct'):
        structs.append(self.parseStructDefinition())
      else:
        funcs.append(self.parseFunctionDefinition())
    return Module(token, decls, structs, funcs)

  def parseType(self):
    token = self.expect('ID')
    name = token.value
    args = []
    if self.consume('['):
      while not self.consume(']'):
        args.append(self.parseType())
        self.consume(',')
    return ParametricType(token, name, args)

  def parseTypePattern(self):
    if self.at('?'):
      token = self.expect('?')
      name = self.expect('ID').value
      return VariableTypePattern(token, name)
    else:
      token = self.expect('ID')
      name = token.value
      args = []
      if self.consume('['):
        while not self.consume(']'):
          args.append(self.parseTypePattern())
          self.consume(',')
      return ParametricTypePattern(token, name, args)

  def parseDeclaration(self):
    token = self.expect('let')
    name = self.expect('ID').value
    self.expect('=')
    expr = self.parseExpression()
    return Declaration(token, name, expr)

  def parseBlockStatement(self):
    token = self.expect('{')
    stmts = []
    while not self.consume('}'):
      stmts.append(self.parseStatement())
    return BlockStatement(token, stmts)

  def parseStatement(self):
    if self.at('if'):
      token = self.expect('if')
      cond = self.parseExpression()
      body = self.parseStatement()
      other = BlockStatement(token, [])
      if self.consume('else'):
        other = self.parseStatement()
      return IfStatement(cond, body, other)
    elif self.at('while'):
      token = self.expect('while')
      cond = self.parseExpression()
      body = self.parseStatement()
      return WhileStatement(token, cond, body)
    elif self.at('{'):
      return self.parseBlockStatement()
    else:
      expr = self.parseExpression()
      return ExpressionStatement(expr.token, expr)

  def parseExpression(self):
    return self.parsePrimaryExpression()

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
      elif self.at('['):
        args = self.parseArguments()
        return CallExpression(token, name, args)
      else:
        return NameExpression(token, name)
    elif self.at('INT'):
      token = self.next()
      return IntExpression(token, token.value)
    elif self.at('FLOAT'):
      token = self.next()
      return FloatExpression(token, token.value)
    elif self.at('STR'):
      token = self.next()
      value = token.value
      return StrExpression(token, value)

    raise common.ParseError(self.peek(), "Expected expression")

### Tests

t = Parser('Int', '<test>').parseType()
assert str(t) == "ParametricType('Int', [])", t

t = Parser('List[Int]', '<test>').parseType()
assert str(t) == "ParametricType('List', [ParametricType('Int', [])])", t

t = Parser('Int', '<test>').parseTypePattern()
assert str(t) == "ParametricTypePattern('Int', [])", t

t = Parser('?T', '<test>').parseTypePattern()
assert str(t) == "VariableTypePattern('T')", t

t = Parser('List[Int]', '<test>').parseTypePattern()
assert (
    str(t) ==
    "ParametricTypePattern('List', [ParametricTypePattern('Int', [])])"), t

stmt = Parser('hi', '<test>').parseStatement()
assert str(stmt) == "ExpressionStatement(NameExpression('hi'))", stmt

expr = Parser('"hi"', '<test>').parseExpression()
assert str(expr) == "StrExpression('hi')", expr

expr = Parser('a = b', '<test>').parseExpression()
assert str(expr) == "AssignExpression('a', NameExpression('b'))", expr

expr = Parser('f[1, 2.1]', '<test>').parseExpression()
assert (
    str(expr) ==
    "CallExpression('f', [IntExpression(1), FloatExpression(2.1)])"), expr

