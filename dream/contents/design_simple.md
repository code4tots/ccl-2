Simple Design
=============

--- Bottom up ---

So I tried to see what things would look like without
inheritance or interfaces and with static and strong typing.
I'm having a hard time coming up with a clean designs...

Note: Even on Level 1 overloading is assumed for functions.
    So function names don't have to be unique, but function names
    together with argument types of a FunctionDefinition should be.
    I can maybe create a level 0 if necessary for name mangling
    function names.

***********************************************
*** Level 1 -                               ***
***********************************************

Type
  name: str

ClassDefinition
  name: str
  attrs: [(str, Type)]

Expression
  type: INT|FLOAT|STR|NAME|CALL|ASSIGN|GETATTR|SETATTR|NEW
  intval: Int?          # INT
  floatval: Float?      # FLOAT
  strval: String?       # STR, NAME, ASSIGN, GETATTR, SETATTR
  typeval: Type?        # NEW
  exprs: [Expression]   # CALL, ASSIGN, SETATTR, NEW

Statement
  type: WHILE|IF|BLOCK|EXPR|BREAK|CONT|RET|LET
  exprs: [Expression]
  stmts: [Statement]

FunctionDefinition
  name: str
  args: [(str, Type)]
  type: Type
  body: Statement

Module
  name: str
  clss: [ClassDefinition]
  funcs: [FunctionDefinition]

***********************************************
*** Level 2 - parametric types              ***
***********************************************

ParametricType
  name: str
  args: [ParametricType]

ParametricClassDefinition
  name: str
  args: [ParametricType]
  attrs: [(str, ParametricType)]

ExpressionWithParametricTypes
  ...
  typeval: ParametricType?
  exprs: [ExpressionWithParametricTypes]

StatementWithParametricTypes
  type: ...
  exprs: [ExpressionWithParametricTypes]
  stmts [StatementWithParametricTypes]

FunctionDefinitionWithParametricTypes
  name: str
  args: [(str, ParametricType)]
  type: ParametricType
  body: StatementWithParametricTypes

ModuleWithParametricTypes
  name: str
  clss: [ParametricClassDefinition]
  funcs: [FunctionDefinitionWithParametricTypes]

***********************************************
*** Level 3 - function templates            ***
***********************************************

ModuleWithFunctionTemplates
  clss: [ParametricClassDefinition]
  funcs: [FunctionTemplate]

FunctionTemplate
  name: str
  args: [(str, TypePattern)]
  rettype: ParametricType
  body: StatementWithParametricTypes

TypePattern
  type: VARIABLE|LITERAL
  args: [TypePattern]  # LITERAL (should be empty when VARIABLE)

-----------------------------------------------------------------

Translating something that is Level 1 into Java is pretty straight forward.

And parsing ModuleWithFunctionTemplates is pretty straight forwards as well.

The only thing I haven't successfully done before is

  ModuleWithFunctionTemplates -> ModuleWithParametricTypes

and

  ModuleWithParametricTypes -> Module


But laying it out like this, it doesn't seem like these transformations
would be so bad.

