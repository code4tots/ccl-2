from ccl import *

### Lexer test

pairs = [(t.type, t.value) if t.value else t.type for t in lex("""
a 'b' 3 def +
""")]

assert pairs == [('ID', 'a'), ('STR', 'b'), ('NUM', 3), 'def', '+'], pairs

### Parser test

def compare(expr, expected):
  if isinstance(expected, dict):
    if not isinstance(expr, expected['type']):
      raise AssertionError(
          'Expected %r but found %r' % (expected['type'], expr))
    for key in set(k for k in expected if k != 'type'):
      try:
        compare(getattr(expr, key), expected[key])
      except AssertionError as e:
        raise AssertionError('In attr %r\n' % key + str(e))
  elif isinstance(expected, list):
    if not isinstance(expr, list):
      raise AssertionError('Expected list but found %r' % expr)
    for i, (a, b) in enumerate(zip(expr, expected)):
      try:
        compare(a, b)
      except AssertionError as e:
        raise AssertionError('On index %d\n' % i + str(e))
    if len(expr) != len(expected):
      raise AssertionError(
          'Expected list of len %d but found %d' % (len(expected), len(expr)))
  else:
    if expr != expected:
      raise AssertionError('Expected %r but found %r' % (expected, expr))

compare(
    Parser('x')._expression(),
    {'type': NameAst, 'name': 'x'})

compare(
    Parser('a.b')._expression(),
    {
        'type': GetAttrAst,
        'expr': {
            'type': NameAst,
            'name': 'a',
        },
        'attr': 'b',
    })

compare(
    Parser('f[1, "hi"]')._expression(),
    {
        'type': CallAst,
        'f': {
            'type': NameAst,
            'name': 'f',
        },
        'args': [
            {
                'type': NumberAst,
                'value': 1,
            },
            {
                'type': StringAst,
                'value': 'hi',
            },
        ],
    })

compare(
    Parser('a = b')._expression(),
    {
        'type': AssignAst,
        'name': 'a',
        'expr': {
            'type': NameAst,
            'name': 'b',
        },
    })

compare(
    Parser('f[1] = "hi"')._expression(),
    {
        'type': CallAst,
        'f': {
            'type': GetAttrAst,
            'expr': {
                'type': NameAst,
                'name': 'f',
            },
        },
        'args': [
            {
                'type': NumberAst,
                'value': 1,
            },
            {
                'type': StringAst,
                'value': 'hi',
            }
        ],
    })

compare(
    Parser('return x')._expression(),
    {
        'type': ReturnAst,
        'expr': {
            'type': NameAst,
            'name': 'x',
        }
    })

compare(
    Parser('break')._expression(),
    {
        'type': BreakAst,
    })

compare(
    Parser('continue')._expression(),
    {
        'type': ContinueAst,
    })

compare(
    Parser('def f[x, *y] 5')._expression(),
    {
        'type': FuncAst,
        'name': 'f',
        'args': ['x'],
        'vararg': 'y',
        'body': {
            'type': NumberAst,
            'value': 5,
        }
    })

compare(
    Parser('class Cls {}')._expression(),
    {
        'type': ClassAst,
        'name': 'Cls',
        'bases': [],
        'varbase': None,
        'body': {
            'type': BlockAst,
            'exprs': [],
        }
    })

compare(
    Parser(r'\x, *y. 5')._expression(),
    {
        'type': FuncAst,
        'name': None,
        'args': ['x'],
        'vararg': 'y',
        'body': {
            'type': ReturnAst,
            'expr': {
                'type': NumberAst,
                'value': 5,
            },
        },
    })

compare(
    Parser('while 1 2')._expression(),
    {
        'type': WhileAst,
        'cond': {'type': NumberAst, 'value': 1},
        'body': {'type': NumberAst, 'value': 2},
    })

compare(
    Parser('if 1 2')._expression(),
    {
        'type': IfAst,
        'cond': {'type': NumberAst, 'value': 1},
        'body': {'type': NumberAst, 'value': 2},
        'other': None,
    })

compare(
    Parser('if 1 2 else 3')._expression(),
    {
        'type': IfAst,
        'cond': {'type': NumberAst, 'value': 1},
        'body': {'type': NumberAst, 'value': 2},
        'other': {'type': NumberAst, 'value': 3},
    })

compare(
    Parser('{}')._expression(),
    {
        'type': BlockAst,
        'exprs': [],
    })

compare(
    Parser('{1}')._expression(),
    {
        'type': BlockAst,
        'exprs': [{'type': NumberAst, 'value': 1}],
    })

compare(
    Parser('not 1')._expression(),
    {
        'type': NotAst,
        'expr': {'type': NumberAst, 'value': 1},
    })

compare(
    Parser('0 and 1')._expression(),
    {
        'type': AndAst,
        'left': {'type': NumberAst, 'value': 0},
        'right': {'type': NumberAst, 'value': 1},
    })

compare(
    Parser('0 or 1')._expression(),
    {
        'type': OrAst,
        'left': {'type': NumberAst, 'value': 0},
        'right': {'type': NumberAst, 'value': 1},
    })

compare(
    Parser('0 == 1')._expression(),
    {
        'type': CallAst,
        'f': {
            'type': GetAttrAst,
            'attr': '__eq__',
            'expr': {'type': NumberAst, 'value': 0},
        },
        'args': [
            {'type': NumberAst, 'value': 1},
        ],
        'vararg': None,
    })
