"""sanity.py

A very simple programming language with zero cost abstractions.

Module
  classes - [Class]
Class
  singleton - Bool
  name - String
  base - String
  members - [String]
  methods - [Method]
Method
  name - String
  args - [String]
  body - Statement
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
    Or
      left - Expression
      right - Expression
    And
      left - Expression
      right - Expression
    Name
      name - String
    Int
      value - Int
    Float
      value - Float
    String
      value - String

"""

class Ast(object):
  def __init__(self, *args):
    if len(type(self).attrs) != len(args):
      raise Exception('expected %d, got %d' % (
          len(type(self).attrs), len(args)))
    for name, value in zip(type(self).attrs, args):
      setattr(self, name, value)

class ModuleAst(Ast):
  attrs = ['classes']

class ClassAst(Ast):
  attrs = ['singleton', 'name', 'base', 'members', 'methods']

class MethodAst(Ast):
  attrs = ['name', 'args', 'body']

class StatementAst(Ast):
  pass

class WhileAst(StatementAst):
  attrs = ['condition', 'body']

  def collect_all_assigned_names(self, names):
    self.condition.collect_all_assigned_names(names)
    self.body.collect_all_assigned_names(names)

class BreakAst(StatementAst):
  attrs = []

  def collect_all_assigned_names(self, names):
    pass

class ContinueAst(StatementAst):
  attrs = []

  def collect_all_assigned_names(self, names):
    pass

class IfAst(StatementAst):
  attrs = ['condition', 'body', 'other']

  def collect_all_assigned_names(self, names):
    self.condition.collect_all_assigned_names(names)
    self.body.collect_all_assigned_names(names)
    self.other.collect_all_assigned_names(names)

class BlockAst(StatementAst):
  attrs = ['statements']

  def collect_all_assigned_names(self, names):
    for statement in self.statements:
      statement.collect_all_assigned_names(names)

class ExpressionAst(StatementAst):
  pass

class AssignAst(ExpressionAst):
  attrs = ['name', 'value']

  def collect_all_assigned_names(self, names):
    names.add(self.name)
    self.value.collect_all_assigned_names(names)

class MethodCallAst(ExpressionAst):
  attrs = ['owner', 'name', 'args']

  def collect_all_assigned_names(self, names):
    self.owner.collect_all_assigned_names(names)
    for arg in self.args:
      arg.collect_all_assigned_names(names)

class OrAst(ExpressionAst):
  attrs = ['left', 'right']

  def collect_all_assigned_names(self, names):
    self.left.collect_all_assigned_names(names)
    self.right.collect_all_assigned_names(names)

class AndAst(ExpressionAst):
  attrs = ['left', 'right']

  def collect_all_assigned_names(self, names):
    self.left.collect_all_assigned_names(names)
    self.right.collect_all_assigned_names(names)

class NameAst(ExpressionAst):
  attrs = ['name']

class IntAst(ExpressionAst):
  attrs = ['value']

class FloatAst(ExpressionAst):
  attrs = ['value']

class StringAst(ExpressionAst):
  attrs = ['value']
