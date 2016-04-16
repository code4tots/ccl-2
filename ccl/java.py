from . import err


def translate(modules):
  return ''.join(map(translate_module, modules))

def translate_module(module):
  pass


class Translator(object):

  def visit_module(self, module):
    pass



