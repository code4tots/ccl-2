KEYWORDS = (
    'include',

    # TODO: Allow classes and methods to be
    # qualified with private or public.
    'private',
    'public',

    'class',

    'for',
    'while',
    'in',
)

SYMBOLS = tuple(reversed(sorted([
    '+',
    '++',
    '=',
    ',',
])))


class Source(object):

  def __init__(self, filespec, string, index):
    self.filespec = filespec
    self.string = string
    self.index = index


class Token(object):

  def __init__(self, type_, value, source):
    self.type = type_
    self.value = value
    self.source = source

  def __repr__(self):
    return 'Token(%r, %r)' % (self.type, self.value)


class Lexer(object):

  ## public

  def lex(self, filespec, string):
    self.filespec = filespec
    self.string = string
    self.position = 0
    self.indent_stack = ['']
    self.tokens = []

    while True:
      self.skip_empty_lines_and_comments()

      if self.at_end():
        break

      self.process_indents()

      while True:
        self.skip_spaces()

        if self.at_line_end():
          break

        self.tokens.append(
            self.lex_number() or
            self.lex_symbol() or
            self.lex_string_literal() or
            self.lex_word_or_keyword() or
            self.raise_unrecognized_token_err())

      self.tokens.append(self.make_token('NEWLINE', None, self.position))

    for _ in range(len(self.indent_stack) - 1):
      self.tokens.append(self.make_token('DEDENT', None, self.position))

    self.tokens.append(self.make_token('EOF', None, self.position))

    return self.tokens

  ## private

  def skip_empty_lines_and_comments(self):
    s = self.string

    while True:
      i = j = self.position
      while j < len(s) and s[j] in ' \t':
        j += 1

      self.position = j
      if self.consume_line_end():
        if self.at_end():
          break
        else:
          continue
      else:
        self.position = i

      break

  def consume_line_end(self):
    i = self.position
    s = self.string

    if i < len(s) and s[i] == '#':
      while i < len(s) and s[i] != '\n':
        i += 1

    if i >= len(s) or s[i] == '\n':
      if s.startswith('\n', i):
        i += 1
      self.position = i
      return True

  def at_end(self):
    return self.position >= len(self.string)

  def at_line_end(self):
    return self.at_end() or self.string[self.position] in '#\n'

  def process_indents(self):
    indent = self.extract_indent()

    if indent == self.indent_stack[-1]:
      pass

    elif indent in self.indent_stack:
      while indent != self.indent_stack[-1]:
        self.tokens.append(self.make_token('DEDENT', None, self.position))
        self.indent_stack.pop()

    elif indent.startswith(self.indent_stack[-1]):
      self.indent_stack.append(indent)
      self.tokens.append(self.make_token('INDENT', None, self.position))

    else:
      # TODO: Better error message.
      raise SyntaxError('Invalid indentation')

  def skip_spaces(self):
    i = self.position
    s = self.string
    while i < len(s) and s[i] in ' \t':
      i += 1
    self.position = i

  def extract_indent(self):
    i = j = self.position
    s = self.string
    while j < len(s) and s[j].isspace():
      j += 1
    return s[i:j]

  def make_token(self, type_, value, index):
    return Token(type_, value, Source(self.filespec, self.string, index))

  def lex_number(self):
    i = self.position
    s = self.string

    seen_digits = False

    if i < len(s) and s[i] in '+-':
      i += 1

    while i < len(s) and s[i].isdigit():
      seen_digits = True
      i += 1

    if i < len(s) and s[i] == '.':
      i += 1

    while i < len(s) and s[i].isdigit():
      seen_digits = True
      i += 1

    if not seen_digits:
      return None

    value = float(s[self.position:i])
    self.position = i

    return self.make_token('NUMBER', value, i)

  def lex_symbol(self):
    for symbol in SYMBOLS:
      if self.string.startswith(symbol, self.position):
        i = self.position
        self.position += len(symbol)
        return self.make_token(symbol, None, i)

  def lex_string_literal(self):
    i = start = self.position
    s = self.string
    raw = False

    if i < len(s) and s[i] == 'r':
      raw = True
      i += 1

    if s.startswith(('"""', "'''"), i):
      q = s[i:i+3]
    elif s.startswith(('"', "'"), i):
      q = s[i]
    else:
      return None
    i += len(q)

    while True:
      if i >= len(s):
        # TODO: Better error message
        raise SyntaxError('Finish string literal')

      if s.startswith(q, i):
        i += len(q)
        break

      if not raw and s[i] == '\\':
        i += 1
        if i >= len(s):
          # TODO: Better error message
          raise SyntaxError('Finish escape character')
        i += 1
      else:
        i += 1

    self.position = i

    return self.make_token('STRING', eval(s[start:i]), start)

  def lex_word_or_keyword(self):
    i = j = self.position
    s = self.string

    if s[j].isalnum() or s[j] == '_':
      while s[j].isalnum() or s[j] == '_':
        j += 1
      word = s[i:j]
      self.position = j

      if word in KEYWORDS:
        return self.make_token(word, None, i)
      else:
        return self.make_token('NAME', word, i)

  def raise_unrecognized_token_err(self):
    i = j = self.position
    s = self.string

    while j < len(s) and not s[j].isspace():
      j += 1

    # TODO: Better error message
    raise SyntaxError('Unrecognized token %r' % s[i:j])


### Lexer tests

tokens = Lexer().lex('<test>', r"""
+5
""")
items = [t.type if t.value is None else (t.type, t.value) for t in tokens]
assert items == [
    ('NUMBER', 5), 'NEWLINE', 'EOF',
], items

tokens = Lexer().lex('<test>', r"""
'hello world!'
""")
items = [t.type if t.value is None else (t.type, t.value) for t in tokens]
assert items == [
    ('STRING', 'hello world!'), 'NEWLINE', 'EOF',
], items

tokens = Lexer().lex('<test>', r"""
class
""")
items = [t.type if t.value is None else (t.type, t.value) for t in tokens]
assert items == [
    'class', 'NEWLINE', 'EOF',
], items

tokens = Lexer().lex('<test>', r"""
some_name
""")
items = [t.type if t.value is None else (t.type, t.value) for t in tokens]
assert items == [
    ('NAME', 'some_name'), 'NEWLINE', 'EOF',
], items

try:
  tokens = Lexer().lex('<test>',
r"""
~
""")
except SyntaxError:
  pass
else:
  # TODO: Better error message
  raise Exception(
      'Expected "~" to raise a syntax erorr, but '
      'succeeded: %r' % tokens)

tokens = Lexer().lex('<test>', r"""
a
  b + c
  d
    e # some comments
  f+g
""")
items = [t.type if t.value is None else (t.type, t.value) for t in tokens]
assert items == [
    ('NAME', 'a'), 'NEWLINE', 'INDENT',
        ('NAME', 'b'), '+', ('NAME', 'c'), 'NEWLINE',
        ('NAME', 'd'), 'NEWLINE', 'INDENT',
            ('NAME', 'e'), 'NEWLINE',
        'DEDENT',
        ('NAME', 'f'), '+', ('NAME', 'g'), 'NEWLINE',
    'DEDENT',
    'EOF',
], items


class Parser(object):

  ## public

  def parse(self, filespec, string):
    self.tokens = Lexer().lex(filespec, string)
    self.position = 0

    return self.parse_module()

  ## private

  def peek(self):
    return self.tokens[self.position]

  def next(self):
    token = self.peek()
    self.position += 1
    return token

  def consume(self, type_):
    if type_ == self.peek().type:
      return self.next()

  def expect(self, type_):
    token = self.consume(type_)
    if not token:
      raise SyntaxError('Expected %s but found %s' % (type_, self.peek()))

  def parse_module(self):
    classes = []
    includes = []
    while not self.consume('EOF'):
      if self.consume('class'):
        classes.append(self.parse_class())
      elif self.consume('include'):
        includes.append(self.parse_include())
      else:
        # TODO: Better error message.
        raise SyntaxError(
            'Expected class or include but found %r' % self.peek())

    return {
        'type': 'module',
        'classes': classes,
        'includes': include,
    }


class Transpiler(object):

  def transpile(self, filespec, string):
    self.parsed_module = Parser().parse(filespec, string)

    self.header = ''
    self.source = ''
    self.method_implementations = ''
    self.static_variable_declarations = ''

    self.generate_header()

