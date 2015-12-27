import common

class Ast(common.Ast):
  pass

###############
### Level 1 ###
###############

class Type(Ast):
  attrs = [('name', str)]

class ClassDefinition(Ast):
  attrs = [('name', str), ('attrs', [(str, Type)])]

class Expression(Ast):
  pass

# 'Expr' is a dummy subclass since I can't refer to 'Expression'
# in the body of its own class definition.
class Expr(Expression):
  attrs = [
      ('type', 'INT FLOAT STR NAME CALL ASSIGN GETATTR SETATTR NEW'),
      ('intval', [int]),
      ('floatval', [float]),
      ('strval', [str]),
      ('typeval', [Type]),
      ('exprs', [Expression]),
  ]

def IntExpr(token, val):
  return Expr(token, 'INT', [val], [], [], [], [])

def FloatExpr(token, val):
  return Expr(token, 'FLOAT', [], [val], [], [], [])

def StrExpr(token, val):
  return Expr(token, 'STR', [], [], [val], [], [])

def NameExpr(token, name):
  return Expr(token, 'NAME', [], [], [name], [], [])

def CallExpr(token, f, args):
  return Expr(token, 'CALL', [], [], [f], [], list(args))

def NewExpr(token, type_):
  return Expr(token, 'NEW', [], [], [], [type_], [])

class Statement(Ast):
  pass

# Same here as with 'Expr'
class Stmt(Statement):
  attrs = [
      ('type', 'WHILE IF BLOCK EXPR BREAK CONT RET LET'),
      ('exprs', [Expression]),
      ('stmts', [Statement]),
  ]

def BlockStmt(token, stmts):
  return Stmt(token, 'BLOCK', [], list(stmts))

def RetStmt(token, expr):
  return Stmt(token, 'RET', [expr], [])

class FunctionDefinition(Ast):
  attrs = [
      ('name', str),
      ('args', [(str, Type)]),
      ('type', Type),
      ('body', Statement),
  ]

class Module(Ast):
  attrs = [
      ('name', str),
      ('clss', [ClassDefinition]),
      ('funcs', [FunctionDefinition]),
  ]

def AstToJava(ast):
  if isinstance(ast, Module):
    return "\npublic class M_%s extends CclCore\n{%s%s\n}" % (
        ast.name,
        ''.join(map(AstToJava, ast.clss)).replace('\n', '\n  '),
        ''.join(map(AstToJava, ast.funcs)).replace('\n', '\n  '))

  elif isinstance(ast, ClassDefinition):
    return "\npublic static class C_%s\n{%s\n}" % (
        ast.name,
        ''.join('\n  public %s A_%s;' % (
            AstToJava(type_), name) for name, type_ in ast.attrs))

  elif isinstance(ast, Type):
    if ast.name == 'Void':
      return 'void'
    elif ast.name == 'Int':
      return 'Integer'
    elif ast.name == 'Float':
      return 'Float'
    else:
      return 'C_' + ast.name

  elif isinstance(ast, FunctionDefinition):
    assert ast.body.type == 'BLOCK', ast.body.type
    return '\npublic static %s f_%s(%s)%s' % (
        AstToJava(ast.type),
        ast.name,
        ', '.join('%s v_%s' % (AstToJava(type_), name)
            for name, type_ in ast.args),
        AstToJava(ast.body))

  elif isinstance(ast, Statement):
    if ast.type == 'BLOCK':
      return '\n{%s\n}' % ''.join(map(
          AstToJava, ast.stmts)).replace('\n', '\n  ')
    elif ast.type == 'RET':
      return '\nreturn %s;' % AstToJava(ast.exprs[0])
    else:
      raise ValueError(ast.type)

  elif isinstance(ast, Expr):
    if ast.type == 'INT':
      return 'Integer.valueOf(%d)' % ast.intval[0]
    elif ast.type == 'FLOAT':
      return 'Float.valueOf(%f)' % ast.floatval[0]
    elif ast.type == 'NAME':
      return 'v_' + ast.strval[0]
    elif ast.type == 'CALL':
      return 'f_%s(%s)' % (
          ast.strval[0],
          ', '.join(map(AstToJava, ast.exprs)))
    elif ast.type == 'NEW':
      return 'new %s()' % AstToJava(ast.typeval[0])
    else:
      raise ValueError(ast.type)

  raise common.TranslationError(
      ast.token, '%r is not translatable' % type(ast))

m = Module(None, 'MyModule', [], [])
s = AstToJava(m)
assert s == r"""
public class M_MyModule extends CclCore
{
}""", s

m = Module(None, 'MyModule', [
    ClassDefinition(None, 'MyClass', []),
], [])
s = AstToJava(m)
assert s == r"""
public class M_MyModule extends CclCore
{
  public static class C_MyClass
  {
  }
}""", s

# TODO: I'm not yet sure what the correct behavior should be when
# member attributes are of a user defined type (should it be like
# it is in Java, or like it is in C/C++? Or something else?).
# Figure it out and adjust accordingly.
m = Module(None, 'MyModule', [
    ClassDefinition(None, 'MyClass', [
        ('myAttr', Type(None, 'MyClass')),
    ]),
], [])
s = AstToJava(m)
assert s == r"""
public class M_MyModule extends CclCore
{
  public static class C_MyClass
  {
    public C_MyClass A_myAttr;
  }
}""", s

m = Module(None, 'MyModule', [], [
    FunctionDefinition(None, 'MyFunc',
      [('x', Type(None, 'Int'))],
      Type(None, 'Int'),
      BlockStmt('BLOCK', [
        RetStmt(None,
          CallExpr(None, 'Add', [
            NameExpr(None, 'x'),
            IntExpr(None, 1),
          ]),
        ),
      ]),
    )
])
s = AstToJava(m)
assert s == r"""
public class M_MyModule extends CclCore
{
  public static Integer f_MyFunc(Integer v_x)
  {
    return f_Add(v_x, Integer.valueOf(1));
  }
}""", s

m = FloatExpr(None, 4.2)
s = AstToJava(m)
assert (
    s ==
    'Float.valueOf(4.200000)'
), s

m = NewExpr(None, Type(None, 'MyClass'))
s = AstToJava(m)
assert (
    s ==
    'new C_MyClass()'
), s


###############
### Level 2 ###
###############


