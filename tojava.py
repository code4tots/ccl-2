import parse


class Context(object):

  def __init__(self):
    self.classes = set()
    self.depth = 0
    self.filesets = dict()


def Translate(string, filename):
  context = Context()
  module = parse.Parse(string, filename)
  return TranslateNode(context, module)


def TranslateNode(context, node):
  if isinstance(node, parse.Module):
    table = dict()
    for cls in node.classes:
      table[cls.name] = TranslateNode(context, cls)
    return table
  elif isinstance(node, parse.Class):
    name = node.name
    interfaces = ', '.join('XXI' + base for base in bases)
    context.depth += 1
    declarations = [TranslateNode(decl)+'\n' for decl in node.declarations]
    methods = [TranslateNode(method) for method in node.methods]
    body = '{\n%s}\n' % ''.join((''.join(declarations), ''.join(methods)))
    context.depth -= 1
    return 'class XXC%s extends XXCObject implements %s\n%s' % (name, interfaces, body)
  elif isinstance(node, parse.Method):
    pass
  else:
    raise TypeError(type(node))


Translate(r"""

class Main : Object
  pass


class Thing : Object
  var x : Int
  var y : Float

  method Run(universe)
    pass

""", '<test>')

