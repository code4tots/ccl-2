from lex import Lexer

class Parser(object):

  ## public

  def parse(self, filespec, string):
    self.tokens = Lexer().lex(filespec, string)
    self.position = 0

    return self.parse_module()

  ## private

  def peek(self):
    return self.tokens[self.position]

  def at(self, type_):
    return type_ == self.peek().type

  def next(self):
    token = self.peek()
    self.position += 1
    return token

  def consume(self, type_):
    if self.at(type_):
      return self.next()

  def expect(self, type_):
    token = self.consume(type_)
    if not token:
      raise SyntaxError('Expected %s but found %s' % (type_, self.peek()))
    return token

  def skip_newlines(self):
    while self.consume('NEWLINE'):
      pass

  def parse_module(self):
    classes = []
    includes = []
    self.skip_newlines()
    while not self.at('EOF'):
      if self.at('class'):
        classes.append(self.parse_class())
      elif self.at('include'):
        includes.append(self.parse_include())
      else:
        # TODO: Better error message.
        raise SyntaxError(
            'Expected class or include but found %r' % self.peek())
      self.skip_newlines()

    return {
        'type': 'module',
        'classes': classes,
        'includes': includes,
    }

  def parse_include(self):
    self.expect('include')
    path = self.expect('STRING').value
    self.expect('NEWLINE')
    return {
        'type': 'include',
        'path': path,
    }


parsed_module = Parser().parse('<test>', r"""
include blarglib

class Blarg
  pass

class Blarg : Object Num

  var x y z

  m()
    return a + b


""")

print(parsed_module)
