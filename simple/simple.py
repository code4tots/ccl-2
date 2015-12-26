"""

# Naming convention:
# types and function names are CamelCase,
# everything else is snake_case.

class List[T] {
  len Int
  cap Int
  buf Array[T]
}

Get[ls List[?T], i Int] T {
  return Get[ls.buf, i]
}

MakeList[a Array[?T]] List[T] {
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
  # # and aggregate types (i.e. 'class') default to setting all its members
  # # to their corresponding default values.
}

F[a ?T, b ?T] T {
  return Add[a, b]
}

Main[args Array[String]] Void {
  F[Get[args, 0], Get[args, 1]]
  Print[ %[1, 2, 3] ]
  Print[ MakeList[%[1, 2, 3]] ]
}

=================

'Main' function is always instantiated.
All other functions are instantiated only if their type version is used.
Types are only instantiated if they are used in a function.

Main -> Function  -> Type
           ^      |
           \-------

=================


Template
(FunctionTemplate, ClassTemplate)

   |
   | instantiate templates
   |
   v

Definition
(FunctionDefinition, ClassDefinition)

At this point, each line of code should basically have a
1-1 correspondence with outputs of the next phase.

   |
   |  name mangling based on type arguments.
   |
   v

Flattened
(FlattenedFunctionDefinition, FlattenedClassDefinition).

All types are plain names and not 'ParametricType'.
All functions are uniquely determined by their name.

-----

Depending on which features I'm ok with, and what platforms
I'm targeting, I could skip the name mangling part.

E.g. with operator overloading and templates/generics.

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

class ClassTemplate(Ast):
  attrs = [
      ('name', str),
      # I've thought about having pattern matching in ClassTemplate
      # arguments the way I let FunctionTemplate argument types to be
      # patterns.
      # It seems like this feature would be nice, orthogonal and
      # consistent with the existing features.
      # However, it seems like it would be more work than doing it
      # the dumb substitution way I'm doing now, and I'm not
      # 100% confident I'm going to finish this as it is, so
      # I'm going to leave this as is for now.
      # TODO: Come back to this.
      ('args', [str]),
      ('attrs', [(str, Type)]),
  ]

class ClassDefinition(Ast):
  attrs = [
      ('name', str),
      # 'args' here really isn't all that important for
      # the content of the class definition, except that
      # the name no longer uniquely identifies the class.
      ('args', [Type]),
      ('attrs', [(str, Type)]),
  ]

class Declaration(Statement):
  attrs = [('name', str), ('expr', Expression)]

class FunctionTemplate(Ast):
  attrs = [
      ('name', str),
      ('args', [(str, TypePattern)]),
      ('type', Type), # return type
      ('body', Statement),
  ]

class FunctionDefinition(Ast):
  attrs = [
      ('name', str),
      ('args', [(str, Type)]),
      ('type', Type),
      ('body', Statement),
  ]

class ModuleTemplate(Ast):
  attrs = [
      ('decls', [Declaration]),
      ('clss', [ClassTemplate]),
      ('funcs', [FunctionTemplate]),
  ]

class Module(Ast):
  attrs = [
      ('decls', [Declaration]),
      ('clss', [ClassDefinition]),
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

class ReturnStatement(Statement):
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

class SetAttributeExpression(Expression):
  attrs = [('expr', Expression), ('name', str), ('val', Expression)]

class GetAttributeExpression(Expression):
  attrs = [('expr', Expression), ('name', str)]

class NameExpression(Expression):
  attrs = [('name', str)]

class StrExpression(Expression):
  attrs = [('val', str)]

class IntExpression(Expression):
  attrs = [('val', int)]

class FloatExpression(Expression):
  attrs = [('val', float)]

class Parser(common.Parser):

  def parseModuleTemplate(self):
    token = self.peek()
    decls = []
    clss = []
    funcs = []
    while not self.at('EOF'):
      if self.at('let'):
        decls.append(self.parseDeclaration())
      elif self.at('class'):
        clss.append(self.parseClassTemplate())
      else:
        funcs.append(self.parseFunctionTemplate())
    return ModuleTemplate(token, decls, clss, funcs)

  def parseClassTemplate(self):
    token = self.expect('class')
    name = self.expect('ID').value
    args = []
    if self.consume('['):
      while not self.consume(']'):
        args.append(self.expect('ID').value)
        self.consume(',')
    self.expect('{')
    attrs = []
    while not self.consume('}'):
      name = self.expect('ID').value
      t = self.parseType()
      attrs.append((name, t))
    return ClassTemplate(token, name, args, attrs)

  def parseFunctionTemplate(self):
    token = self.expect('ID')
    name = token.value
    args = []
    self.expect('[')
    while not self.consume(']'):
      name = self.expect('ID').value
      p = self.parseTypePattern()
      self.consume(',')
      args.append((name, p))
    ret = self.parseType()
    body = self.parseBlockStatement()
    return FunctionTemplate(token, name, args, ret, body)

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
    elif self.at('return'):
      token = self.expect('return')
      expr = self.parseExpression()
      return ReturnStatement(token, expr)
    elif self.at('{'):
      return self.parseBlockStatement()
    else:
      expr = self.parseExpression()
      return ExpressionStatement(expr.token, expr)

  def parseExpression(self):
    return self.parsePostfixExpression()

  def parseArguments(self):
    self.expect('[')
    args = []
    while not self.consume(']'):
      args.append(self.parseExpression())
      self.consume(',')
    return args

  def parsePostfixExpression(self):
    expr = self.parsePrimaryExpression()
    while self.at('.'):
      token = self.next()
      name = self.expect('ID').value
      if self.consume('='):
        val = self.parseExpression()
        expr = SetAttributeExpression(token, expr, name, val)
      else:
        expr = GetAttributeExpression(token, expr, name)
    return expr

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

c = Parser('Main[] Void {}', '<test>').parseFunctionTemplate()
assert (
    str(c) ==
    "FunctionTemplate("
        "'Main', [], ParametricType('Void', []), BlockStatement([]))"), c

c = Parser(
    'Main[args List[String]] Int {'
      'return 0'
    '}', '<test>').parseFunctionTemplate()
assert (
    str(c) ==
    "FunctionTemplate("
        "'args', "
        "[("
            "'args', "
            "ParametricTypePattern("
                "'List', "
                "[ParametricTypePattern('String', [])]))], "
        "ParametricType('Int', []), "
        "BlockStatement([ReturnStatement(IntExpression(0))]))"), c

c = Parser('class C {}', '<test>').parseClassTemplate()
assert str(c) == "ClassTemplate('C', [], [])", c

c = Parser('class C[D] { a Int }', '<test>').parseClassTemplate()
assert (
    str(c) ==
    "ClassTemplate('a', ['D'], [('a', ParametricType('Int', []))])"), c

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

stmt = Parser('return hi', '<test>').parseStatement()
assert str(stmt) == "ReturnStatement(NameExpression('hi'))", stmt

expr = Parser('"hi"', '<test>').parseExpression()
assert str(expr) == "StrExpression('hi')", expr

expr = Parser('a = b', '<test>').parseExpression()
assert str(expr) == "AssignExpression('a', NameExpression('b'))", expr

expr = Parser('f[1, 2.1]', '<test>').parseExpression()
assert (
    str(expr) ==
    "CallExpression('f', [IntExpression(1), FloatExpression(2.1)])"), expr

expr = Parser('a.b', '<test>').parseExpression()
assert (
    str(expr) ==
    "GetAttributeExpression(NameExpression('a'), 'b')"), expr

expr = Parser('a.b = c', '<test>').parseExpression()
assert (
    str(expr) ==
    "SetAttributeExpression(NameExpression('a'), 'b', NameExpression('c'))"
), expr

m = Parser('Main[args ?_] Int { return 0 }', '<test>').parseModuleTemplate()
assert (
    str(m) ==
      "ModuleTemplate([], [], "
      "[FunctionTemplate('args', [('args', VariableTypePattern('_'))], "
        "ParametricType('Int', []), "
        "BlockStatement([ReturnStatement(IntExpression(0))]))])"
), m

class InstantiationError(common.TranslationError):
  pass

# TODO: This is a hack. Think about what you've done.
MAIN_INSTANTIATION_TOKEN = common.Lexer('StartAtMain', '<builtin>').next()

class Instantiator(object):

  def __init__(self, module):
    self.module = module
    self.clss = dict() # (str, [Type]) -> ClassDefinition
    self.funcs = dict() # (str, [Type]) -> FunctionDefinition
    self.clsstodo = dict() # (str, [Type]) -> {Token}
    self.funcstodo = dict() # (str, [Type]) -> {Token}

  def instantiateModule(self):
    """Instantiate 'Main[] Void', the classes used in global declarations,
    and all other things that must be instantiated because of them.
    """
    self.registerFunctionToInstantiate(MAIN_INSTANTIATION_TOKEN, 'main', [])

    # TODO: Global decls
    if self.module.decls:
      raise Exception('Global declarations not yet supported')

    while True:
      if self.clsstodo:
        self.instantiateAnyClass()
      elif self.funcstodo:
        self.instantiateAnyFunction()
      else:
        break

    return Module(
        self.module.token, [],
        list(self.clss.values()),
        list(self.funcs.values()))

  def registerFunctionToInstantiate(self, token, name, argtypes):
    """Mark a function with given argument types as something to instantiate.
    The token argument should point to the location where this instantiation
    takes place, used for error messages.
    """
    key = (name, argtypes)
    if key not in self.funcs:
      if key not in self.funcstodo:
        self.funcstodo[key] = set()
      self.funcstodo[key].add(token)

  def registerClassToInstantiate(self, token, name, argtypes):
    "Like registerFunctionToInstantiate, but for classes instead of functions."
    key = (name, argtypes)
    if key not in self.clss:
      if key not in self.clsstodo:
        self.clsstodo[key] = set()
      self.clsstodo[key].add(token)

  def instantiateFunction(self, name, argtypes):
    """Find a matching FunctionTemplate of given name and argument types,
    and return a FunctionDefinition.
    """
    pass # TODO


