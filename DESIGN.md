DESIGN
======

Just a simple map so that if I decide to burn some of the code,
it's going to be easier for me to do some codereuse.

Tier N can compile with only the classes from Tier 1 ... N as dependencies.

Tier 1 (Lexer)
  Lexer
  Token
  SyntaxError

Tier 2 (Parser)
  Parser
  Ast
  AstVisitor
  Traceable

Tier 3 (Object model & evaluation)
  Val
  Err
  Evaluator
  Scope

Tier 4 (Desktop API)
  Desktop
