import parse


def TranslateStatement(node, depth):
  if isinstance(node, parse.Module):
    return '  ' * depth + ''.join(TranslateStatement(cls, depth) for cls in node.classes)
  elif isinstance(node, parse.Class):
    bases = ['CC' + base for base in node.bases]
    if not bases:
      bases.append('CCObject')
    return '  ' * depth + 'class CC' + node.name + '(' + ', '.join(bases) + '):\n' + ''.join(TranslateStatement(method, depth+1) for method in node.methods)
  elif isinstance(node, parse.Method):
    args = ['XX' + arg for arg, _ in node.arguments]
    return '\n  ' * depth + 'def MM' + node.name + '(XXthis' + ''.join(', ' + arg for arg in args) + '):\n' + TranslateStatement(node.body, depth+1)
  elif isinstance(node, parse.Declaration):
    return '  ' * depth + 'pass\n'
  elif isinstance(node, parse.StatementBlock):
    if node.statements:
      return ''.join(TranslateStatement(s, depth) for s in node.statements)
    else:
      return '  ' * depth + 'pass\n'
  elif isinstance(node, parse.Return):
    pass
  elif isinstance(node, parse.If):
    pass
  elif isinstance(node, parse.While):
    return '  ' * depth + 'while ' + TranslateExpression(node.test) + ':\n' + TranslateStatement(node.body, depth+1)
  elif isinstance(node, parse.Break):
    return '  ' * depth + 'break\n'
  elif isinstance(node, parse.Expression):
    return '  ' * depth + TranslateExpression(node) + '\n'

  raise TypeError('Unrecognized node type %s' % type(node))


def TranslateExpression(node):
  if isinstance(node, parse.Number):
    return 'CCNumber(%r)' % node.value
  elif isinstance(node, parse.String):
    return 'CCString(%r)' % node.value
  elif isinstance(node, parse.VariableLookup):
    return 'XX' + node.name
  elif isinstance(node, parse.New):
    return 'CC%s(%s)' % (node.class_, ', '.join(map(TranslateExpression, node.arguments)))
  elif isinstance(node, parse.GetAttribute):
    return '%s.AA%s' % (TranslateExpression(node.owner, node.attribute))
  elif isinstance(node, parse.SetAttribute):
    return '%s.AA%s = %s' % (TranslateExpression(node.owner), node.attribute, TranslateExpression(node.value))
  elif isinstance(node, parse.MethodCall):
    return '%s.MM%s(%s)' % (TranslateExpression(node.owner), node.attribute, ', '.join(map(TranslateExpression, node.arguments)))
  elif isinstance(node, parse.Assignment):
    return 'XX%s = %s' % (node.name, TranslateExpression(node.value))

  raise TypeError('Unrecognized node type %s' % type(node))


translation = TranslateStatement(parse.Parse(r"""

class Main

  method Run(universe)
    var world

    world = new SimplifiedUniverse(universe)

    this.Hello()
    world.Print(this)
    world.Print("hello world!")

  method Hello()
    pass

""", '<test>'), 0)


class CCObject(object):
  pass


class CCNumber(CCObject):

  def __init__(self, value):
    self.value = value

  def MMAdd(self, other):
    return self.value + other.value


class CCString(CCObject):

  def __init__(self, value):
    self.value = value

  def __str__(self):
    return self.value


class CCUniverse(CCObject):
  pass


class CCSimplifiedUniverse(CCObject):

  def __init__(self, universe):
    self.universe = universe

  def MMPrint(self, string):
    print(string)


print(translation)
exec(translation + "\nCCMain().MMRun(CCUniverse())\n")


fn = 'lex.ccl'

with open(fn) as f:
  print(TranslateStatement(parse.Parse(f.read(), fn)))


