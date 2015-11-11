# TODO: Clean up this crap too.
# By clean up, I mean, think through the design more.
from lex import Lexer


class Parser(object):

  ## public

  def parse(self, filespec, string):
    self.init(filespec, string)
    return self.parse_module()

  ## private

  def init(self, filespec, string):
    self.tokens = Lexer().lex(filespec, string)
    self.position = 0
    return self

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
      # TODO: Better error message (e.g. include location)
      raise SyntaxError('Expected %s but found %s' % (type_, self.peek()))
    return token

  def skip_newlines(self):
    while self.consume('NEWLINE'):
      pass

  ## Actual parsing methods.

  def parse_module(self):
    classes = []
    includes = []
    funcs = []
    self.skip_newlines()
    while not self.at('EOF'):
      if self.at('include'):
        includes.append(self.parse_include())
      elif self.at('class'):
        classes.append(self.parse_class())
      else:
        raise SyntaxError(
            'Expected class include or function but found %r' %
            self.peek())
      self.skip_newlines()
    return {
        'type': 'module',
        'classes': classes,
        'includes', includes,
    }

  def parse_include(self):
    self.expect('include')
    module_name = self.expect('NAME').value
    self.expect('NEWLINE')
    return module_name

  def parse_class(self):
    methods = []
    attrs = []
    self.expect('class')
    class_name = self.expect('NAME').value
    while not 
    self.expect('NEWLINE')
    self.expect('INDENT')
    self.skip_newlines()
    while not self.consume('DEDENT'):
      if self.consume('pass'):
        self.expect('NEWLINE')
      elif self.at('def'):
        methods.append(self.parse_method())
      elif self.consume('var'):
        while not self.consume('NEWLINE'):
          attrs.append(self.expect('NAME').value)
      else:
        raise SyntaxError()
      self.skip_newlines()
    return {
        'type': 'class',
        'methods': methods,
        'attrs': attrs,
    }
