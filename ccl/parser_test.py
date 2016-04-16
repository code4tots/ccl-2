# TODO: Make tests more legible and less fragile.

from . import lexer
from . import parser

def main(verbose):
  source = lexer.Source('<test>',
r"""
include 'include.ccl'

class Foo {
  var bar
  fn baz[a] {}
  fn baz2[a, *b] {}
}

fn main[] {
  print['hi']
  print[5 + 7]
  a.b[c]
}""")
  tree = parser.parse(source)

  assert repr(tree) == """('module', (include, 'include')@1, [('include', (include, 'include')@1, 'include.ccl')], [('class', (class, 'class')@24, 'Foo', 'Object', [('decl', (var, 'var')@38, 'bar', None)], [('func', (fn, 'fn')@48, 'baz', ['a'], None, ('block', ({, '{')@58, [])), ('func', (fn, 'fn')@63, 'baz2', ['a'], 'b', ('block', ({, '{')@78, []))])], [('func', (fn, 'fn')@84, 'main', [], None, ('block', ({, '{')@94, [('expr', (ID, 'print')@98, ('mcall', ([, '[')@103, ('getvar', (ID, 'print')@98, 'print'), '_call_', ('args', ([, '[')@103, [('str', (STR, "'hi'")@104, 'hi')], None))), ('expr', (ID, 'print')@112, ('mcall', ([, '[')@117, ('getvar', (ID, 'print')@112, 'print'), '_call_', ('args', ([, '[')@117, [('mcall', (+, '+')@120, ('num', (NUM, '5')@118, 5.0), '_add_', ('args', (+, '+')@120, [('num', (NUM, '7')@122, 7.0)], None))], None))), ('expr', (ID, 'a')@127, ('mcall', (., '.')@128, ('getvar', (ID, 'a')@127, 'a'), 'b', ('args', ([, '[')@130, [('getvar', (ID, 'c')@131, 'c')], None)))]))], [])""", repr(tree)

  if verbose:
    print('parser_test pass')


if __name__ == '__main__':
  main(1)
