from . import lexer
from . import err

def match(tokens, patterns):
  for tok, (type_, value) in zip(tokens, patterns):
    assert tok.type == type_, (type_, tok)
    assert tok.value == value, (value, tok)

def main(verbose):
  toks = lexer.lex(lexer.Source('<test>', '''while ab "c" 'd' 3\nf==='''))
  match(toks, [
      ('while', 'while'),
      ('ID', 'ab'),
      ('STR', '"c"'),
      ('STR', "'d'"),
      ('NUM', '3'),
      ('ID', 'f'),
      ('==', '=='),
      ('=', '='),
      ('EOF', None),
  ])
  assert toks[0].lineno() == 1, toks[0].lineno()  # while
  assert toks[-1].lineno() == 2, toks[-1].lineo()  # ID(f)

  try:
    lexer.lex(lexer.Source('<test>', '''a\n  ~~'''))
  except err.Err as e:
    assert len(e.tokens) == 1, e.tokens
    assert e.tokens[0].line() == '  ~~', e.tokens[0].line()
    assert e.tokens[0].lineno() == 2, e.tokens[0].lineno()
    assert e.tokens[0].colno() == 3, e.tokens[0].colno()
  else:
    raise Exception('Expected lex of "~~" to raise unrecognized token')

  if verbose:
    print('lexer_test pass')

if __name__ == '__main__':
  main(1)
