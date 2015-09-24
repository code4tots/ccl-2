"""
types
  num (python float)
  str (python str)
  list (python list)
  dict (python dict)
  lambda (represented as tuple of (ctx, node) pair)
  builtin (represented as python object none of the above)

builtins define their own behavior when retrieving or assigning
attributes, or when trying to call it like a function.

"""


class ControlFlowException(Exception):
  def __init__(self, return_value):
    self.return_value = return_value


class ReturnException(ControlFlowException):
  pass


class BreakException(ControlFlowException):
  pass


def find_containing_context(ctx, name):
  if node['name'] in ctx:
    return ctx
  elif '__parent__' in ctx:
    return find_containing_context(ctx['__parent__'], name)

  raise NameError('Name %s is not defined' % node['name'])


def assign(ctx, target, value):
  if target['type'] == 'name':
    find_containing_context(ctx, target['name'])[target['name']] = value
  elif target['type'] == 'list':
    for tar, val in zip(target['items'], value):
      assign(ctx, tar, val)
  elif target['type'] == 'attr':
    raise NotImplemented('Assigning to attributes is not yet supported')
  else:
    raise ValueError('Assigning to %s is not supported' % target['type'])


def run(ctx, node):

  ## variable stuff
  if node['type'] == 'name':
    return find_containing_context(ctx, name)[name]
  elif node['type'] == 'assign':
    value = run(ctx, node['value'])
    assign(ctx, node['target'], value)
    return value
  elif node['type'] == 'declare':
    if node['name'] in ctx:
      raise NameError('Name %s is already declared' % node['name'])
    ctx[node['name']] = 0.0
    return 0.0

  ## constructors
  elif node['type'] in ('num', 'str'):
    return node['value']
  elif node['type'] == 'list':
    return [run(ctx, item) for item in node['items']]
  elif node['type'] == 'dict':
    return {run(ctx, key):run(ctx, value) for key, value in node['items']}
  elif node['type'] == 'lambda':
    return (ctx, node) # see 'call' for more details.

  ## block
  elif node['type'] == 'block':
    last = 0
    for statement in node['statements']:
      last = run(ctx, statement)
    return last

  ## control flow
  elif node['type'] == 'call':
    f = run(ctx, node['function'])
    args = [run(ctx, arg) for arg in node['arguments']]
    try:
      if isinstance(f, tuple):
        fctx, fnode = f
        ectx = {'__parent__': fctx}
        for argname, argval in zip(fnode['arguments'], args):
          ectx[argname] = argval
        return run(ectx, fnode['body'])
      else:
        return f(*args)
    except ReturnException as e:
      return e.return_value
  elif node['type'] == 'attr':
    raise NotImplemented('attributes are not yet supported')
  elif node['type'] == 'if':
    return run(ctx, node['body' if run(ctx, node['test']) else 'other'])
  elif node['type'] == 'while':
    last = 0
    try:
      while run(ctx, node['test']):
        last = run(ctx, node['body'])
    except BreakException as e:
      return e.return_value
    return last
  elif node['type'] == 'break':
    raise BreakException(run(ctx, node['value']) if node.get('value', 0) else 0)
  elif node['type'] == 'return':
    raise ReturnException(run(ctx, node['value']) if node.get('value', 0) else 0)

  raise ValueError('Unrecognized node type: ' + node['type'])

