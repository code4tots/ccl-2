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

Tier 3 (Object model)
  Val

Tier 4 (Evaluation)
  Evaluator
  Scope

Tier 5 (Desktop API)
  Desktop
