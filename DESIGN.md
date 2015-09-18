# DESIGN

### Grammar

Module                 : ModuleElement* END
                       ;
ModuleElement          : IncludeDeclaration
                       | ClassDefinition
                       ;
IncludeDeclaration     : 'include' STRING=uri
                       ;
ClassDefinition        : 'class' NAME (':' ClassReference*)? INDENT ClassElement* DEDENT
                       ;
ClassElement           : VariableDeclaration
                       | MethodDefinition
                       ;
VariableDeclaration    : 'var' NameTypePair (',' NameTypePair)*
                       ;
NameTypePair           : NAME (':' TYPE)?
                       ;
MethodDefinition       : 'method' NAME '(' (NAME (',' NAME)*)? ')' StatementBlock
                       ;
StatementBlock         : INDENT Statement* DEDENT DELIMITER
                       ;
Statement              : VariableDeclaration
                       | While
                       | Break
                       | Continue
                       | If
                       | Return
                       | Expression DELIMITER
                       ;
While                  : 'while' Expression StatementBlock
                       ;
Break                  : 'break' DELIMITER
                       ;
Continue               : 'continue' DELIMITER
                       ;
If                     : 'if' Expression DELIMITER StatementBlock ('else' StatementBlock)? DELIMITER
                       ;
Return                 : 'return' Expression DELIMITER
                       ;
Expression             : TernaryExpression
                       ;
TernaryExpression      : OrExpression 'if' Expression 'else' TernaryExpression
                       | OrExpression
                       ;
OrExpression           : AndExpression ('or' AndExpression)*
                       ;
AndExpression          : PrefixExpression ('and' PrefixExpression)*
                       ;
PrefixExpression       : 'not' PostfixExpression
                       | '-' PostfixExpression
                       | '+' PostfixExpression
                       | PostfixExpression
                       ;
PostfixExpression      : PrimaryExpression '.' NAME '(' ArgumentList ')'
                       | PrimaryExpression '.' NAME '=' Expression
                       | PrimaryExpression '.' NAME
                       | PrimaryExpression
                       ;
PrimaryExpression      : NAME '=' Expression
                       | NAME '(' ArgumentList ')'
                       | NAME
                       | NUMBER
                       | STRING
                       | '[' ArgumentList ']'
                       | '(' Expression ')'
                       | 'not' Expression
                       ;
