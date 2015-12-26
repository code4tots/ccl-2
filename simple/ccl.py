import common

class Ast(common.Ast):
  pass

class Type(Ast):
  pass

class ParametricType(Type):
  attrs = [('name', str), ('args', [(str, Type)])]

class TypePattern(Ast):
  pass

class ParametricTypePattern(TypePattern):
  attrs = [('name', str), ('args', [(str, TypePattern)])]

class VariableTypePattern(TypePattern):
  attrs = [('name', str)]

class Expression(Ast):
  pass

class AssignExpression(Expression):
  attrs = [('name', str), ('expr', Expression)]

class CallExpression(Expression):
  attrs = [('f', str), ('args', [Expression])]

class NameExpression(Expression):
  attrs = [('name', str)]

class StrExpression(Expression):
  attrs = [('val', str)]

class IntExpression(Expression):
  attrs = [('val', int)]

class FloatExpression(Expression):
  attrs = [('val', float)]

class Statement(Ast):
  pass

class DeclarationStatement(Statement):
  attrs = [('name', str), ('expr', Expression)]

class ExpressionStatement(Statement):
  attrs = [('expr', Expression)]

class ReturnStatement(Statement):
  attrs = [('expr', Expression)]

class IfStatement(Statement):
  attrs = [('cond', Expression), ('body', Statement), ('other', Statement)]

class WhileStatement(Statement):
  attrs = [('cond', Expression), ('body', Statement)]

class BlockStatement(Statement):
  attrs = [('stmts', [Statement])]

class ClassTemplate(Ast):
  attrs = [
      ('name', str),
      ('args', [str]),
      ('attrs', [(str, Type)]),
  ]

class ClassDefinition(Ast):
  """ClassDefinition is not the result of any direct parse.
  It results only from expanding a ClassTemplate."""
  attrs = [
      ('name', str),
      ('attrs', [(str, Type)]),
  ]

class FunctionTemplate(Ast):
  attrs = [
      ('name', str),
      ('args', [(str, TypePattern)]),
      ('type', Type),
      ('body', BlockStatement),
  ]

class FunctionDefinition(Ast):
  """Like ClassDefinition, FunctionDefinition cannot be created
  directly from a parse. FunctionDefinition instances are created by
  expanding a FunctionTemplate."""
  attrs = [
      ('name', str),
      ('args', [(str, Type)]),
      ('type', Type),
      ('body', BlockStatement),
  ]

class Module(Ast):
  attrs = [
      ('ftemps', [FunctionTemplate]),
      ('ctemps', [ClassTemplate]),
  ]

def BindSinglePattern(pat, t, d):
  assert type(t) == ParametricType

  if isinstance(pat, ParametricTypePattern):
    if pat.name != t.name or len(pat.args) != len(t.args):
      return False
    else:
      for l, r in zip(pat.args, t.args):
        if not BindSinglePattern(l, r, d):
          return False
      else:
        return True

  elif isinstance(pat, VariableTypePattern):
    d[pat.name] = t
    return True

  raise type(pat)

def BindTypePatterns(pats, ts):
  assert len(pats) == ts
  typebnds = dict()
  if len(pats) != len(ts):
    return None

  for l, r in zip(pats, ts):
    if not BindSinglePattern(l, r, typebnds):
      return None

  return typebnds

def SubstituteTemplateTypes(t, typebnds):

  assert False, type(t)

def ExpandFunctionTemplate(ftemp, argtypes):
  typebnds = BindTypePatterns([t for _, t in ftemp.args], argtypes)
  assert typebnds is not None
  return FunctionDefinition(
      ftemp.token, ftemp.name,
      list(zip([n for n, _ in ftemp], argtypes)),
      SubstituteTemplateTypes(ftemp.type, typebnds),
      SubstituteTemplateTypes(ftemp.body, typebnds))
