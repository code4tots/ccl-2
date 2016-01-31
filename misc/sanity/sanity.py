"""sanity.py

A very simple programming language with zero cost abstractions.

Class
  name - String
  typeargs - [Type]
  members - [Declaration]
  methods - [Function]
Function
  name - String
  typeargs - [Type]
  args - [String]
  body - Statement
Declaration
  name - String
  type - Type
Statement
  While
    condition - Expression
    body - Statement
  Break
  Continue
  If
    condition - Expression
    body - Statement
    other - Statement
  Block
    statements - [Statement]
  Expression
    Assign
      name - String
      value - Expression
    MethodCall
      owner - Expression
      name - String
      args - [Expression]
    FunctionCall
      name - String
      args - [Expression]
    Or
      left - Expression
      right - Expression
    And
      left - Expression
      right - Expression

"""

class Ast(object):
  def __init__(self, *args):
    if len(type(self).attrs) != len(args):
      raise Exception('expected %d, got %d' % (
          len(type(self).attrs), len(args)))
    for name, value in zip(type(self).attrs, args):
      setattr(self, name, value)

class ClassAst(Ast):
  attrs = ['name', 'typeargs', 'members', 'methods']

class FunctionAst(Ast):
  attrs = ['name', 'typeargs', 'args', 'body']

class DeclarationAst(Ast):
  attrs = ['name', 'type']

class StatementAst(Ast):
  pass

class WhileAst(StatementAst):
  attrs = ['condition', 'body']

class BreakAst(StatementAst):
  attrs = []

class ContinueAst(StatementAst):
  attrs = []

class IfAst(StatementAst):
  attrs = ['condition', 'body', 'other']

class BlockAst(StatementAst):
  attrs = ['statements']

class ExpressionAst(StatementAst):
  pass

class AssignAst(ExpressionAst):
  attrs = ['name', 'value']

class 