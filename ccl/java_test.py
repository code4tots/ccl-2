from . import lexer
from . import parser
from . import java

def main(verbose):
  java.translate([parser.parse(lexer.Source('<test>', r"""
"""))])
  if verbose:
    print('java_test pass')


if __name__ == '__main__':
  main(1)

