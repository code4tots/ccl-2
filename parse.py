"""parse.py"""

from lex import *


class Node(object):

  def __init__(self, origin, *args):
    assert origin is None or isinstance(origin, Origin), type(origin)
    assert len(self.attributes) == len(args), (type(self), self.attributes, args)
    self.origin = origin
    for name, value in zip(self.attributes, args):
      setattr(self, name, value)

  def __eq__(self, other):
    if type(self) != type(other):
      return False
    for name in self.attributes:
      if getattr(self, name) != getattr(other, name):
        return False
    return True

  def __repr__(self):
    return '%s(None, %s)' % (type(self).__name__, ', '.join(repr(getattr(self, attr)) for attr in self.attributes))


class Module(Node):
  "sequence of classes"
  attributes = ['classes']


class Class(Node):
  "sequence of bases, declarations and methods"
  attributes = ['name', 'bases', 'declarations', 'methods']


class Method(Node):
  "method signature and block of statements"
  attributes = ['name', 'arguments', 'return_type', 'body']


class Statement(Node):
  "Declaration|Expression|While|Break|If|Pass|Return"


class Declaration(Statement):
  "variable name and optionally type name"
  attributes = ['name', 'type']


class StatementBlock(Statement):
  "sequence of statements"
  attributes = ['statements']


class Return(Statement):
  "return expression"
  attributes = ['expression']


class If(Statement):
  "test expression, if statement, and optionally else statement"
  attributes = ['test', 'body', 'other']


class While(Statement):
  "test expression and block of statements"
  attributes = ['test', 'body']


class Break(Statement):
  attributes = []


class Expression(Node):
  "VariableLookup|GetAttribute|MethodCall|Assignment|SetAttribute|Number|String"


class Number(Expression):
  attributes = ['value']


class String(Expression):
  attributes = ['value']


class VariableLookup(Expression):
  "variable name"
  attributes = ['name']


class GetAttribute(Expression):
  "owner expression, attribute name"
  attributes = ['owner', 'attribute']


class SetAttribute(Expression):
  "owner expression, attribute name and value expression"
  attributes = ['owner', 'attribute', 'value']


class MethodCall(Expression):
  "owner expression, attribute name and argument expressions"
  attributes = ['owner', 'attribute', 'arguments']


class Assignment(Expression):
  "variable name and value expression"
  attributes = ['name', 'value']


class ParseError(Exception):

  def __init__(self, message, origin):
    super(ParseError, self).__init__(message + '\n' + origin.LocationMessage())


def Parse(string, filename):
  toks = Lex(string, filename)
  i = [0]

  def Peek(lookahead=0):
    return toks[i[0]+lookahead]

  def At(type_, origin=None, lookahead=0):
    if Peek(lookahead).type == type_:
      if origin:
        origin[0] = Peek(lookahead).origin
      return True

  def GetToken():
    tok = toks[i[0]]
    i[0] += 1
    return tok

  def Consume(type_, origin=None):
    if At(type_, origin):
      return GetToken()

  def Expect(type_, origin=None):
    if not At(type_, origin):
      raise ParseError('Expected %s but found %s' % (type_, Peek().type), Peek().origin)
    return GetToken()

  def EatStatementDelimiters():
    while Consume('Newline') or Consume(';'):
      pass

  def ParseModule():
    EatStatementDelimiters()
    clss = [ParseClass()]
    while not At('End'):
      clss.append(ParseClass())
      EatStatementDelimiters()
    return Module(clss[0].origin, clss)

  def ParseClass():
    origin = [None]
    Expect('class', origin)
    name = Expect('Name').value
    bases = []
    declarations = []
    methods = []
    while At('Name'):
      bases.append(GetToken().value)
    EatStatementDelimiters()
    Expect('Indent')
    EatStatementDelimiters()
    while not Consume('Dedent'):
      if At('var'):
        declarations.append(ParseDeclaration())
      elif At('method'):
        methods.append(ParseMethod())
      elif Consume('pass'):
        pass
      else:
        raise ParseError('Expected declaration or method', Peek().origin)
      EatStatementDelimiters()
    return Class(origin[0], name, bases, declarations, methods)

  def ParseDeclaration():
    origin = [None]
    Expect('var', origin)
    name = Expect('Name').value
    type_ = None
    if Consume(':'):
      type_ = Expect('Name').value
    return Declaration(origin[0], name, type_)

  def ParseMethod():
    origin = [None]
    Expect('method', origin)
    name = Expect('Name').value
    args = []
    Expect('(')
    while not Consume(')'):
      arg = Expect('Name').value
      argtype = None
      if Consume(':'):
        argtype = Expect('Name').value
      args.append((arg, argtype))
      Consume(',')
    rettype = None
    if Consume(':'):
      rettype = Expect('Name').value
    EatStatementDelimiters()
    body = ParseStatementBlock()
    return Method(origin[0], name, args, rettype, body)

  def ParseStatementBlock():
    origin = [None]
    Expect('Indent', origin)
    EatStatementDelimiters()
    statements = []
    while not Consume('Dedent'):
      if Consume('pass'):
        pass
      else:
        statements.append(ParseStatement())
      EatStatementDelimiters()
    return StatementBlock(origin[0], statements)

  def ParseStatement():
    origin = [None]
    if At('Indent'):
      return ParseStatementBlock()
    elif At('var'):
      return ParseDeclaration()
    elif Consume('return', origin):
      return Return(origin[0], ParseExpression())
    elif Consume('if', origin):
      test = ParseExpression()
      EatStatementDelimiters()
      body = ParseStatement()
      other = None
      EatStatementDelimiters()
      if Consume('else'):
        other = ParseStatement()
      elif Peek(-1).type in (';', 'Newline'): # TODO: Find more elegant solution.
        i[0] -= 1
      return If(origin[0], test, body, other)
    else:
      return ParseExpression()

  def ParseExpression():
    return ParsePostfixExpression()

  def ParsePostfixExpression():
    origin = [None]
    expr = ParsePrimaryExpression()
    while True:
      if Consume('.', origin):
        attr = Expect('Name').value
        if At('(', origin):
          args = ParseMethodCallArguments()
          expr = MethodCall(origin[0], expr, attr, args)
        elif Consume('=', origin):
          value = ParseExpression()
          expr = SetAttribute(origin[0], expr, attr, value)
        else:
          expr = GetAttribute(origin[0], expr, attr)
      else:
        break
    return expr

  def ParsePrimaryExpression():
    origin = [None]
    if At('Name', origin):
      name = GetToken().value
      if Consume('=', origin):
        value = ParseExpression()
        return Assignment(origin[0], name, value)
      elif At('(', origin):
        args = ParseMethodCallArguments()
        return MethodCall(origin[0], None, name, args)
      else:
        return VariableLookup(origin[0], name)
    elif At('Number', origin):
      return Number(origin[0], GetToken().value)
    elif At('String', origin):
      return String(origin[0], GetToken().value)
    else:
      raise ParseError('Expected expression', Peek().origin)

  def ParseMethodCallArguments():
    Expect('(')
    args = []
    while not Consume(')'):
      args.append(ParseExpression())
      Consume(',')
    return args

  return ParseModule()


### Tests

module = Parse(r"""

class Main
  pass

""", '<test>')

assert module == Module(None, [
  Class(None, 'Main', [], [], []),
]), module

module = Parse(r"""

class Main
  method Main()
    pass

""", '<test>')

assert module == Module(None, [
  Class(None, 'Main', [],
    [],
    [
      Method(None, 'Main', [], None, StatementBlock(None, [])),
    ]),
]), module

module = Parse(r"""

class Main

  var x
  var y : Int

  method Main()
    var z : Float

""", '<test>')

assert module == Module(None, [
  Class(None, 'Main', [],
    [
      Declaration(None, 'x', None),
      Declaration(None, 'y', 'Int'),
    ],
    [
      Method(None, 'Main', [], None, StatementBlock(None,
        [
          Declaration(None, 'z', 'Float'),
        ])),
    ]),
]), module

module = Parse(r"""

class Main

  var x
  var y : Int

  method Main()
    var z : Float

  method Add(a, b)
    return a.Add(b)

""", '<test>')

assert module == Module(None, [
  Class(None, 'Main', [],
    [
      Declaration(None, 'x', None),
      Declaration(None, 'y', 'Int'),
    ],
    [
      Method(None, 'Main', [], None, StatementBlock(None,
        [
          Declaration(None, 'z', 'Float'),
        ])
      ),
      Method(None, 'Add', [('a', None), ('b', None)], None, StatementBlock(None,
        [
          Return(None,
            MethodCall(None,
              VariableLookup(None, 'a'),
              'Add',
              [VariableLookup(None, 'b')],
            )
          ),
        ])
      ),
    ]),
]), module

module = Parse(r"""

class Main

  var x
  var y : Int

  method Main()
    var z : Float

  method Add(a : Int, b : Int) : Int
    return a.Add(b)

""", '<test>')

assert module == Module(None, [
  Class(None, 'Main', [],
    [
      Declaration(None, 'x', None),
      Declaration(None, 'y', 'Int'),
    ],
    [
      Method(None, 'Main', [], None, StatementBlock(None,
        [
          Declaration(None, 'z', 'Float'),
        ])
      ),
      Method(None, 'Add', [('a', 'Int'), ('b', 'Int')], 'Int', StatementBlock(None,
        [
          Return(None,
            MethodCall(None,
              VariableLookup(None, 'a'),
              'Add',
              [VariableLookup(None, 'b')],
            )
          ),
        ])
      ),
    ]),
]), module
