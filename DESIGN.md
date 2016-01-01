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

Tier 3 (eval dir -- Object model and Evaluation)
  Val
    Func
      UserFunc
      BuiltinFunc
    Num
    Str
    List
    Map
  Err
  Traceable
  Evaluator
  Scope

Tier 4 (Desktop API)
  Desktop
