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
  body - [Statement]
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
