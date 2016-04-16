import os

from . import err
from . import lexer
from . import parser

# TODO: Improve loader to load from remote git repositories.

class Loader(object):

  def __init__(self, roots):
    self.roots = list(roots)
    if os.curdir not in self.roots:
      self.roots.append(os.curdir)

  def read(self, uri):
    for root in self.roots:
      path = os.path.join(self.root, uri)
      if os.path.exists(path):
        with open(path) as f:
          return f.read()
    raise err.Err('Could not find %r (%r)' % (uri, path))

  def load_only(self, uri):
    return parser.parse(lexer.Source(uri, self.read(uri)))

  def load(self, uri):
    stack = [uri]
    seen = set([uri])
    mods = []
    while stack:
      m = self.load_only(stack.pop())
      mods.append(m)
      for inc in m[2]:
        if inc not in seen:
          seen.add(inc)
          stack.append(inc)
    return mods
