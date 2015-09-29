"""translator.py"""

import sys

### Lexer

SYMBOLS = (
  '(', ')', '[', ']', '{', '}',
  '.', '=', ',', ';', ':', '\\',
)
KEYWORDS = (
  'class', 'lambda',
  'pass',
  'var',
  'return',
  'while', 'break', 'continue',
  'if', 'else',
  'nil',
  'true', 'false',
)


class Origin(object):

  def __init__(self, filespec, string, position):
    self.filespec = filespec
    self.string = string
    self.position = position

  def LocationMessage(self):
    return 'in %s on line %d column %d\n%s\n%s*\n' % (
        self.filespec,
        self.LineNumber(),
        self.ColumnNumber(),
        self.Line(),
        ' ' * (self.ColumnNumber() - 1))

  def LineNumber(self):
    return 1 + self.string.count('\n', 0, self.position)

  def ColumnNumber(self):
    return 1 + self.position - self.LineStart()

  def Line(self):
    return self.string[self.LineStart():self.LineEnd()]

  def LineStart(self):
    return self.string.rfind('\n', 0, self.position) + 1

  def LineEnd(self):
    p = self.string.find('\n', self.position)
    return len(self.string) if p == -1 else p


class Token(object):

  def __init__(self, origin, type, value=None):
    self.origin = origin
    self.type = type
    self.value = value

  def __eq__(self, other):
    return self.type == other.type and self.value == other.value

  def __repr__(self):
    return 'Token(%r,%r)' % (self.type, self.value)


class CclError(Exception):

  def __init__(self, origin, message):
    super(CclError, self).__init__(message + '\n' + origin.LocationMessage())


class LexError(CclError):
  pass


def Lex(filespec, string):
  s = string
  i = 0
  j = 0
  indent_stack = ['']
  depth = 0
  tokens = []

  def MakeOrigin():
    return Origin(filespec, s, j)

  def MakeToken(type, value=None):
    return Token(MakeOrigin(), type, value)

  while True:
    "skips newlines iff depth > 0"
    while i < len(s) and ((s[i].isspace() and (depth or s[i] != '\n')) or s[i] == '#'):
      if s[i] == '#':
        while i < len(s) and s[i] != '\n':
          i += 1
      else:
        i += 1

    if i >= len(s):
      break

    j = i

    if s[i] == '\n':
      i += 1
      tokens.append(MakeToken('Newline'))
      while True:
        j = i
        while i < len(s) and s[i].isspace() and s[i] != '\n':
          i += 1
        if i < len(s) and s[i] == '#':
          while i < len(s) and s[i] != '\n':
            i += 1
        if i >= len(s) or not s[i].isspace():
          break
        i += 1
      if i < len(s):
        indent = s[j:i]
        if indent == indent_stack[-1]:
          pass
        elif indent.startswith(indent_stack[-1]):
          tokens.append(MakeToken('Indent'))
          tokens.append(MakeToken('Newline'))
          indent_stack.append(indent)
        elif indent in indent_stack:
          while indent != indent_stack[-1]:
            tokens.append(MakeToken('Dedent'))
            tokens.append(MakeToken('Newline'))
            indent_stack.pop()
        else:
          raise LexError(MakeOrigin(), 'Invalid indent: ' + repr(indent))

    elif s[i].isdigit() or s[i] == '.' and s[i+1:i+2].isdigit():
      while i < len(s) and s[i].isdigit():
        i += 1
      if i < len(s) and s[i] == '.':
        i += 1
      while i < len(s) and s[i].isdigit():
        i += 1
      tokens.append(MakeToken('Number', float(s[j:i])))

    elif s.startswith(('r"', "r'", '"', "'"), i):
      raw = False
      if s[i] == 'r':
        i += 1
        raw = True
      quote = s[i:i+3] if s.startswith(('"""', "'''"), i) else s[i]
      i += len(quote)
      while not s.startswith(quote, i):
        if i >= len(s):
          raise LexError(MakeOrigin(), "Missing quotes for: " + quote)
        i += 2 if not raw and s[i] == '\\' else 1
      i += len(quote)
      tokens.append(MakeToken('String', eval(s[j:i])))

    elif s.startswith(SYMBOLS, i):
      symbol = max(symbol for symbol in SYMBOLS if s.startswith(symbol, i))
      if symbol in ('(', '{', '['):
        depth += 1
      elif symbol in (')', '}', ']'):
        depth -= 1
      i += len(symbol)
      tokens.append(MakeToken(symbol))

    elif s[i].isalnum() or s[i] == '_':
      while s[i].isalnum() or s[i] == '_':
        i += 1
      word = s[j:i]
      if word in KEYWORDS:
        tokens.append(MakeToken(word))
      else:
        tokens.append(MakeToken('Name', word))

    else:
      while i < len(s) and not s[i].isspace():
        i += 1
      raise LexError(MakeOrigin(), "Unrecognized token: " + s[j:i])

  while indent_stack[-1] != '':
    tokens.append(MakeToken('Dedent', None))
    indent_stack.pop()

  tokens.append(MakeToken('End'))

  return tokens

### Lexer Tests

origin = Origin('<test>', """
hello world!
""", 1)

assert origin.Line() == 'hello world!', repr(origin.Line())

assert origin.LocationMessage() == """in <test> on line 2 column 1
hello world!
*
""", repr(origin.LocationMessage())

tokens = Lex('<test>', """
"hello".Print()
""")

assert (
    tokens ==
    [
        Token(None, 'Newline', None),
        Token(None, 'String', 'hello'),
        Token(None, '.', None),
        Token(None, 'Name', 'Print'),
        Token(None, '(', None),
        Token(None, ')', None),
        Token(None, 'Newline', None),
        Token(None, 'End', None),
    ]
), tokens

tokens = Lex('<test>', """
i = 0
while i.LessThan(10)
  i.Print()
  i = i.Add(1)
""")

assert (
    tokens ==
    [
        Token(None, 'Newline', None),

        Token(None, 'Name', 'i'),
        Token(None, '=', None),
        Token(None, 'Number', 0.0),
        Token(None, 'Newline', None),

        Token(None, 'while', None),
        Token(None, 'Name', 'i'),
        Token(None, '.', None),
        Token(None, 'Name', 'LessThan'),
        Token(None, '(', None),
        Token(None, 'Number', 10),
        Token(None, ')', None),
        Token(None, 'Newline', None),

        Token(None, 'Indent', None),

          Token(None, 'Newline', None),

          Token(None, 'Name', 'i'),
          Token(None, '.', None),
          Token(None, 'Name', 'Print'),
          Token(None, '(', None),
          Token(None, ')', None),
          Token(None, 'Newline', None),

          Token(None, 'Name', 'i'),
          Token(None, '=', None),
          Token(None, 'Name', 'i'),
          Token(None, '.', None),
          Token(None, 'Name', 'Add'),
          Token(None, '(', None),
          Token(None, 'Number', 1),
          Token(None, ')', None),
          Token(None, 'Newline', None),

        Token(None, 'Dedent', None),

        Token(None, 'End', None),
    ]
), tokens

try:
  Lex('<test>', '!@#')
except LexError as e:
  assert str(e) == """Unrecognized token: !@#
in <test> on line 1 column 1
!@#
*
""", str(e)
else:
  assert False, "Lex('!@#', '<test>') should have raised error but didn't"

### Parser

class ParseError(CclError):
  pass


class Parser(object):

  def __init__(self, filespec, string):
    self.filespec = filespec
    self.string = string
    self.tokens = Lex(filespec, string)
    self.i = 0

  def GetToken(self):
    token = self.tokens[self.i]
    self.i += 1
    return token

  def Peek(self, lookahead=0):
    return self.tokens[self.i + lookahead]

  def At(self, type, origin_pointer=None):
    if self.Peek().type == type:
      if origin_pointer:
        origin_pointer[0] = self.Peek().origin
      return True

  def Consume(self, type, origin_pointer=None):
    if self.At(type, origin_pointer):
      return self.GetToken()

  def Expect(self, type, origin_pointer=None):
    if not self.At(type, origin_pointer):
      raise ParseError(self.Peek(0).origin, 'Expected %s but found %s' % (type, self.Peek(0).type))
    return self.GetToken()

  def EatStatementDelimiters(self):
    while self.Consume('Newline') or self.Consume(';'):
      pass

  def ParseModule(self):
    statements = []
    origin = self.Peek().origin
    self.EatStatementDelimiters()
    while not self.Consume('End'):
      statements.append(self.ParseStatement())
      self.EatStatementDelimiters()
    return {'type': 'Module', 'stmts': statements, 'origin': origin}

  def ParseStatement(self):
    origin_pointer = [None]
    if self.Consume('if', origin_pointer):
      test = self.ParseExpression()
      body = self.ParseStatementBlock()
      other = None
      if self.Consume('else'):
        if self.At('if'):
          other = self.ParseStatement()
        else:
          other = self.ParseStatementBlock()
      return {'type': 'If', 'test': test, 'body': body, 'other': other, 'origin': origin_pointer[0]}
    elif self.Consume('while', origin_pointer):
      test = self.ParseExpression()
      body = self.ParseStatementBlock()
      return {'type': 'While', 'test': test, 'body': body, 'origin': origin_pointer[0]}
    elif self.Consume('break', origin_pointer):
      return {'type': 'Break', 'origin': origin_pointer[0]}
    elif self.Consume('continue', origin_pointer):
      return {'type': 'Continue', 'origin': origin_pointer[0]}
    elif self.Consume('return', origin_pointer):
      expr = None
      if not self.At('Newline'):
        expr = self.ParseExpression()
      return {'type': 'Return', 'expr': expr, 'origin': origin_pointer[0]}
    elif self.Consume('var', origin_pointer):
      decls = []
      while True:
        target = self.ParsePostfixExpression()
        value = None
        if self.Consume('='):
          value = self.ParseExpression()
        decls.append({'type': 'Declaration', 'target': target, 'value': value, 'origin': target['origin']})
        if not self.Consume(','):
          break
      return {'type': 'Block', 'stmts': decls, 'origin': origin_pointer}
    else:
      expr = self.ParseExpression()
      return {'type': 'Expression', 'expr': expr, 'origin': origin_pointer[0]}

  def ParseStatementBlock(self):
    origin_pointer = [None]
    self.EatStatementDelimiters()
    self.Expect('Indent', origin_pointer)
    self.EatStatementDelimiters()
    statements = []
    while not self.Consume('Dedent'):
      statements.append(self.ParseStatement())
      self.EatStatementDelimiters()
    self.EatStatementDelimiters()
    return {'type': 'Block', 'stmts': statements, 'origin': origin_pointer[0]}

  def ParseExpression(self):
    return self.ParseAssignExpression()

  def ParseAssignExpression(self):
    return self.ParseExponentiationExpression()

  def ParseExponentiationExpression(self):
    origin_pointer = [None]
    expr = self.ParsePostfixExpression()
    if self.Consume('**', origin_pointer):
      right = self.ParseExponentiationExpression()
      return {'type': '__pow__', 'left': expr, 'right': right, 'origin': origin_pointer[0]}
    return expr

  def ParsePostfixExpression(self):
    origin_pointer = [None]
    expr = self.ParsePrimaryExpression()
    while True:
      if self.Consume('(', origin_pointer):
        args = []
        while not self.Consume(')'):
          args.append(self.ParseExpression())
          self.Consume(',')
        expr = {'type': '__call__', 'f': expr, 'args': args, 'origin': origin_pointer[0]}
      elif self.Consume('.', origin_pointer):
        attr = self.Expect('Name').value
        expr = {'type': '__getattr__', 'owner': expr, 'attr': attr, 'origin': origin_pointer[0]}
      elif self.Consume('[', origin_pointer):
        index = self.ParseExpression()
        expr = {'type': '__getitem__', 'owner': expr, 'i': index, 'origin': origin_pointer[0]}
      else:
        break
    return expr

  def ParsePrimaryExpression(self):
    origin_pointer = [None]
    if self.Consume('('):
      expr = self.ParseExpression()
      self.Consume(')')
      return expr
    elif self.At('Name', origin_pointer):
      return {'type': 'Name', 'name': self.Expect('Name').value, 'origin': origin_pointer[0]}
    elif self.Consume('nil', origin_pointer):
      return {'type': 'nil', 'origin': origin_pointer[0]}
    elif self.Consume('true', origin_pointer):
      return {'type': 'true', 'origin': origin_pointer[0]}
    elif self.Consume('false', origin_pointer):
      return {'type': 'false', 'origin': origin_pointer[0]}
    elif self.At('Number', origin_pointer):
      return {'type': 'Number', 'value': self.Expect('Number').value, 'origin': origin_pointer[0]}
    elif self.At('String', origin_pointer):
      return {'type': 'String', 'value': self.Expect('String').value, 'origin': origin_pointer[0]}
    elif self.Consume('[', origin_pointer):
      items = []
      while not self.Consume(']'):
        items.append(self.ParseExpression())
        self.Consume(',')
      return {'type': 'List', 'items': items, 'origin': origin_pointer[0]}
    elif self.Consume('{', origin_pointer):
      pairs = []
      while not self.Consume('}'):
        key = self.ParseExpression()
        value = None
        if self.Consume(':'):
          value = self.ParseExpression()
        self.Consume(',')
        pairs.append((key, value))
      return {'type': 'Dict', 'pairs': pairs, 'origin': origin_pointer[0]}
    elif self.Consume('\\', origin_pointer):
      args = []
      vararg = None
      while self.At('Name'):
        args.append(self.Expect('Name').value)
        self.Consume(',')
      if self.Consume('*'):
        vararg = self.Expect('Name').value
      if self.Consume('.'):
        body = self.ParseExpression()
      else:
        body = self.ParseStatementBlock()
      return {'type': 'Lambda', 'args': args, 'vararg': vararg, 'body': body, 'origin': origin_pointer[0]}
    else:
      raise ParseError(self.Peek().origin, 'Expected expression but found ' + self.Peek().type)


def Parse(filespec, string):
  return Parser(filespec, string).ParseModule()

### Parser Tests

def Diff(left, right):
  if type(left) != type(right):
    return 'left side is type %s but right side is type %s' % tuple(map(type, (left, right)))
  elif left is None:
    return '' if right is None else 'left is None but right is %s' % right
  elif type(left) == dict:
    left = dict(left)
    right = dict(right)
    left.pop('origin', None)
    right.pop('origin', None)
    if left['type'] != right['type']:
      return 'Type of left is %s but type of right is %s' % (left['type'], right['type'])
    for key in left.keys():
      if key not in right:
        return 'Key %r is in left but not in right' % key
    for key in right.keys():
      if key not in left:
        return 'Key %r is in right but not in left' % key
    for key in left.keys():
      diff = Diff(left[key], right[key])
      if diff:
        return 'For key element %s\n%s' % (key, diff)
    return ''
  elif type(left) == float:
    return '' if left == right else 'left is %f but right is %f' % (left, right)
  elif type(left) == str:
    return '' if left == right else 'left is %s but right is %s' % (left, right)
  elif type(left) == tuple:
    i = 0
    while i < min(len(left), len(right)):
      diff = Diff(left[i], right[i])
      if diff:
        return 'On tuple element %d\n' % i + diff
      i += 1
    if len(left) < len(right):
      return 'left has no more elements but right has %s' % right[i]
    if len(right) < len(left):
      return 'right has no more elements but left has %s' % left[i]
    return ''
  elif type(left) == list:
    i = 0
    while i < min(len(left), len(right)):
      diff = Diff(left[i], right[i])
      if diff:
        return 'On list element %d\n' % i + diff
      i += 1
    if len(left) < len(right):
      return 'left has no more elements but right has %s' % right[i]
    if len(right) < len(left):
      return 'right has no more elements but left has %s' % left[i]
    return ''
  return 'unrecognized type %s' % type(left)

node = Parse('<test>', '')
diff = Diff(node, {'type': 'Module', 'stmts': []})
assert not diff, diff

node = Parse('<test>', r"""
hello
""")
diff = Diff(node, {'type': 'Module', 'stmts': [
    {'type': 'Expression', 'expr': {'type': 'Name', 'name': 'hello'}},
]})
assert not diff, diff

node = Parse('<test>', r"""
hello(1, 2, 3)
""")
diff = Diff(node, {'type': 'Module', 'stmts': [
    {'type': 'Expression', 'expr': {'type': '__call__',
        'f': {'type': 'Name', 'name': 'hello'},
        'args': [
            {'type': 'Number', 'value': 1.0},
            {'type': 'Number', 'value': 2.0},
            {'type': 'Number', 'value': 3.0},
        ]}},
]})
assert not diff, diff

node = Parse('<test>', r"""
if 1
  2
else
  3
""")
diff = Diff(node, {'type': 'Module', 'stmts': [
    {'type': 'If',
        'test': {'type': 'Number', 'value': 1.0},
        'body': {'type': 'Block', 'stmts':[
            {'type': 'Expression', 'expr': {'type': 'Number', 'value': 2.0}},
        ]},
        'other': {'type': 'Block', 'stmts': [
            {'type': 'Expression', 'expr': {'type': 'Number', 'value': 3.0}},
        ]},
    },
]})
assert not diff, diff

node = Parse('<test>', r"""
while 1
  continue
  break
  [1, 2, 3]
""")
diff = Diff(node, {'type': 'Module', 'stmts': [
    {'type': 'While',
        'test': {'type': 'Number', 'value': 1.0},
        'body': {'type': 'Block', 'stmts':[
            {'type': 'Continue'},
            {'type': 'Break'},
            {'type': 'Expression', 'expr': {'type': 'List', 'items': [
                {'type': 'Number', 'value': 1.0},
                {'type': 'Number', 'value': 2.0},
                {'type': 'Number', 'value': 3.0},
            ]}}
        ]},
    },
]})
assert not diff, diff

node = Parse('<test>', r"""
var x = \ a b c
  return x
""")
diff = Diff(node, {'type': 'Module', 'stmts': [
    {'type': 'Block', 'stmts': [
        {'type': 'Declaration',
            'target': {'type': 'Name', 'name': 'x'},
            'value': {'type': 'Lambda', 'args': ['a', 'b', 'c'], 'vararg': None,
                'body': {'type': 'Block', 'stmts': [
                    {'type': 'Return', 'expr': {'type': 'Name', 'name': 'x'}},
                ]}}}
    ]},
]})
assert not diff, diff


### ToJava

def SanitizeStringForJava(string):
  return '"%s"' % string.replace('"', '\\"').replace('\n', '\\n')

def TranslateNodeToJava(node):
  if node is None:
    return 'NIL'
  elif type(node) == float:
    return 'X(%f)' % str(node)
  elif type(node) == str:
    return 'X(%s)' % SanitizeStringForJava(node)
  elif type(node) == list:
    return 'A(%s)' % ', '.join(map(TranslateNodeToJava, node))
  elif type(node) == dict:
    return 'D(%s)' % ', '.join('%s, %s' % tuple(map(TranslateNodeToJava, (k, v))) for k, v in node.items())
  elif type(node) == Origin:
    return 'D(X("filespec"), FILESPEC, X("source"), SOURCE, X("position"), X(%d))' % node.position
  else:
    raise TypeError('Unrecognized TranslateNodeToJava type %s (%r)' % (type(node), node))


def TranslateModuleToJava(filespec, string):
  node = Parse(filespec, string)
  return r"""
    {
      final Obj FILESPEC = X(%s);
      final Obj SOURCE = X(%s);
      CODE_REGISTRY.getattr("__setitem__").call(FILESPEC, %s);
    }
    """ % (SanitizeStringForJava(filespec), SanitizeStringForJava(string), TranslateNodeToJava(node))

JAVA_TEMPLATE = r"""
public class Ccl extends Xccl {

  public Ccl() {
    super(X(%s));
%s
  }

  public static void main(String[] args) {
    new Ccl().run();
  }
}
"""

def TranslateToJava(filespec, string):
  # TODO: Multiple modules.
  return JAVA_TEMPLATE % (SanitizeStringForJava(filespec), TranslateModuleToJava(filespec, string))


translation = TranslateToJava('<test>', r"""
var main = \
  print("Hello world!")

main()

""")

print(translation)

