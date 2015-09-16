"""importer.py"""

import parse


class Program(parse.Node):
  attributes = ['main_module', 'uri_to_modules']


def ResolveImports(main_module):
  uri_to_modules = dict()

  queue = set([main_module])
  while queue:
    module = queue.pop()
    for import_ in module.imports:
      if import_.uri not in uri_to_modules:
        uri_to_modules[import_.uri] = ImportModule(import_.uri)
        queue.add(import_.uri)

  return Program(main_module.origin, main_module, uri_to_modules)


def ImportModule(uri):
  if uri.startswith('local:'):
    path = uri[len('local:'):]
    with open(path) as f:
      content = f.read()
    return parse.Parse(content, path)
  elif uri.startswith('github:'):
    raise ValueError('importing from github not yet supported')
  else:
    raise SyntaxError('Unsupported import uri scheme: ' + uri)


### Tests

"""
Testing the importer would require mocking file system, and that's a bit
tedious without some setup.

Also, at least for now, the code is simple enough that I think I can get away
without testing.
"""
