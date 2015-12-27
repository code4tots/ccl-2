Simple Design
=============

--- Bottom up ---

So I tried to see what things would look like without
inheritance or interfaces and with static and strong typing.
I'm having a hard time coming up with a clean designs...

Note: Even on Level 1 overloading is assumed for functions.
    So function names don't have to be unique, but function names
    together with argument types of a FunctionDefinition shoudl be.

***********************************************
*** Level 1 -                               ***
***********************************************

Program
  clss: [ClassDefinition]
  funcs: [FunctionDefinition]

ClassDefinition
  name: str
  attrs: [(str, Type)]

Type
  name: str

FunctionDefinition
  name: str
  args: [(str, Type)]
  rettype: Type
  body: Statement

Statement
  type: WHILE|IF|BLOCK|EXPR|BREAK|CONT|RET
  exprs: [Expression]
  stmts: [Statement]

Expression
  type: INT|FLOAT|STR|NAME|CALL|ASSIGN|GETATTR|SETATTR|NEW
  intval: Int?          # INT
  floatval: Float?      # FLOAT
  strval: String?       # STR, NAME, ASSIGN, GETATTR, SETATTR
  type: Type?           # NEW
  exprs: [Expression]   # CALL, ASSIGN, SETATTR, NEW

***********************************************
*** Level 2 - parametric types              ***
***********************************************

ProgramWithParametricTypes
  clss: [ParametricClassDefinition]
  funcs: [FunctionDefinitionWithParametricTypes]

ParametricClassDefinition
  name: str
  args: [ParametricType]
  attrs: [(str, ParametricType)]

ParametricType
  name: str
  args: [ParametricType]

FunctionDefinitionWithParametricTypes
  name: str
  args: [(str, ParametricType)]
  rettype: ParametricType
  body: StatementWithParametricType

StatementWithParametricType
  ...

ExpressionWithParametricType
  ...

***********************************************
*** Level 3 - function templates            ***
***********************************************

ProgramWithFunctionTemplates
  clss: [ParametricClassDefinition]
  funcs: [FunctionTemplate]

FunctionTemplate
  name: str
  args: [(str, TypePattern)]
  rettype: ParametricType
  body: StatementWithParametricType

TypePattern
  type: VARIABLE|LITERAL
  args: [TypePattern]  # LITERAL (should be empty when VARIABLE)

-----------------------------------------------------------------

Translating something that is Level 1 into Java is pretty straight forward.

And parsing ProgramWithFunctionTemplates is pretty straight forwards as well.

The only thing I haven't successfully done before is

  ProgramWithFunctionTemplates -> ProgramWithParametricTypes

and

  ProgramWithParametricTypes -> Program


But laying it out like this, it doesn't seem like these transformations
would be so bad.

