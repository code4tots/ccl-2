# DESIGN

### Grammar

Module : ModuleElement* END
       ;
ModuleElement : ImportDeclaration
              | ClassDefinition
              ;
ImportDeclaration : 'import' STRING=uri NAME
                  ;
ClassDefinition : 'class' NAME (':' ClassReference*)? INDENT ClassElement* DEDENT
                ;
ClassElement : VariableDeclaration
             | MethodDefinition
             ;
VariableDeclaration : 'var' NameTypePair (',' NameTypePair)*
                    ;
NameTypePair : NAME (':' TYPE)?
             ;
MethodDefinition : 'method' NAME '(' (NAME (',' NAME)*)? ')' StatementBlock
                 ;
StatementBlock : INDENT Statement* DEDENT
               ;
Statement : VariableDeclaration
          | While
          | Break
          | Continue
          | If
          | Return
          | Expression
          ;
While : 'while' Expression StatementBlock
      ;
Break : 'break'
      ;
Continue : 'continue'
         ;
If : 'if' Expression StatementBlock ('else' StatementBlock)?
   ;
Return : 'return' Expression
       ;
Expression : # TODO
           ;
