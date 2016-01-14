class Token(object):
  def __init__(self, lexer, i, type_, value):
    self.lexer = lexer
    self.i = i
    self.type = type_
    self.value = value



