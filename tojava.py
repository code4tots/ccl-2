"""tojava.py"""
import parse

NATIVE_PRELUDE = r"""
class CclClassObject {

}

"""


class TranslationError(Exception):

  def __init__(self, message, origin):
    super(TranslationError, self).__init__(message + '\n' + origin.LocationMessage())


def Translate(program):
  if len(program.modules) != 1:
    raise ValueError('importing modules is not yet supported')

  return TranslateModule(program.modules['<main>'])


def TranslateModule(module):
  if module.imports:
    raise TranslationError('importing modules is not yet supported', module.imports[0].origin)

  return NATIVE_PRELUDE + ''.join(map(TranslateClass, module.classes))


def TranslateClass(node):
  if len(node.bases) > 1:
    raise TranslationError('multiple inheritance is not yet supported', node.origin)

  if node.bases:
    base = node.bases[0]
  else:
    base = 'Object'

  declarations = ''.join(TranslateStatement(d, 1) for d in node.declarations)
  methods = ''.join(TranslateStatement(d, 1) for d in node.methods)

  return 'class CclClass%s extends CclClass%s {\n%s%s}\n' % (node.name, base, declarations, methods)

def TranslateStatement(node, depth):
  if isinstance(node, parse.Method):
    return '%spublic CclClassObject CclMethod%s(%s)\n%s' % (
        '  ' * depth,
        node.name,
        ', '.join('CclClassObject CclVariable' + n for n, _ in node.arguments),
        TranslateStatement(node.body, depth))
  elif isinstance(node, parse.Declaration):
    return '%sCclClassObject CclVariable%s;\n' ('  ' * depth, node.name)
  elif isinstance(node, parse.StatementBlock):
    return ('  ' * depth + '{\n' +
            ''.join(TranslateStatement(s, depth+1) for s in node.statements) +
            '  ' * depth + '}\n')
  elif isinstance(node, parse.Return):
    return '  ' * depth + TranslateExpression(node.expression) + '\n'
  elif isinstance(node, parse.If):
    pass
  elif isinstance(node, parse.While):
    pass
  elif isinstance(node, parse.Break):
    return '  ' * depth + 'break\n'
  elif isinstance(node, parse.Expression):
    return '  ' * depth + TranslateExpression(node) + '\n'

  raise TypeError('Unrecognized statement node type: %s' % type(node))


def TranslateExpression(node):
  if isinstance(node, parse.Number):
    pass
  elif isinstance(node, parse.String):
    pass
  elif isinstance(node, parse.VariableLookup):
    pass
  elif isinstance(node, parse.New):
    pass
  elif isinstance(node, parse.GetAttribute):
    pass
  elif isinstance(node, parse.SetAttribute):
    pass
  elif isinstance(node, parse.MethodCall):
    pass
  elif isinstance(node, parse.Assignment):
    pass

  raise TypeError('Unrecognized expression node type: %s' % type(node))


### Tests

print(TranslateModule(parse.Parse(r"""

class Main

  method Run()
    pass

""", '<test>')))

