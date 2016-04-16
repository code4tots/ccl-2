class Err(Exception):
  def __init__(self, message, *tokens):
    self.tokens = list(tokens)
    self.message = message

  def add_token(self, token):
    self.tokens.append(token)

  def __str__(self):
    return repr(self)

  def __repr__(self):
    return 'err: %s%s' % (
        self.message,
        ''.join(map(location_message, self.tokens)))

def location_message(token):
  return '\nin file "%s" on line %d %r:\n%s\n%s' % (
      token.source.filespec,
      token.lineno(),
      token,
      token.line(),
      (token.colno()-1) * ' ' + '*')
