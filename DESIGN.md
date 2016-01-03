DESIGN
======

Just a simple map so that if I decide to burn some of the code,
it's going to be easier for me to do some codereuse.

Tier N can compile with only the classes from Tier 1 ... N as dependencies.

Tier 1 (grammar dir -- Lexer)
  Lexer
  Token
  SyntaxError

Tier 2 (grammar dir -- Parser)
  Parser
  Ast
  AstVisitor

Tier 3 (val dir and eval dir -- Object model and Evaluation)
  val
    Val
    Func
    UserFunc
    BuiltinFunc
    Num
    Str
    List
    Map
  eval
    Err
    Traceable
    Evaluator
    Scope      ** Designated initializer.

  # In terms of filling up the global scope, all of it
  # should be initialized inside 'Scope', since I'm afraid that if
  # I fragment the initialization code into each of the classes,
  # a lot of it might not run properly since instantiating 'Scope'
  # does not necessarily mean that all of the 'Val' subclasses
  # will be initialized.
  #
  # If you require that all initial modifications to GLOBAL
  # be done inside of Scope, you know that GLOBAL will have
  # everything it needs once an instance of Scope is initialized.
  #
  # If you need to add more to your global scope, consider using a proxy
  # global scope (e.g. See Desktop.DESKTOP_GLOBAL).

Tier 4 (Desktop API)
  Desktop      ** Designated initializer
