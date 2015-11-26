"""cpl.py
Contest Programming Language.

Translates cpl to C++.

methods/operators on all objects
  & (pointer to object)
  and (logical and operator)
  or (logical or operator)
  xor (logical xor operator)
  == (equality)

Builtin types (and methods/operators)
  str
    + % size [num] [num]=
    lines split

  num
    + - * / % < > <= >=

Builtin meta-types (and methods/operators)
  tuple
    [num] (get element)
    [num]= (assign element)
    size (compile time constant)
  list
    [num] (get element)
    [num]= (assign element)
    size
    each
    map
    reduce
    filter
    dict (must be a list of 2-tuples)
  dict
    [] (get element)
    []= (assign element)
    size
    map
    filter
    list (generates a list of (k,v) 2-tuples)
  ptr
    * (derefernce operator)
  func
    [...] (function call)

CPL has no user defined types or meta-types.

Builtin functions
  read
  print

TODO: Use https://gcc.gnu.org/onlinedocs/cpp/Line-Control.html
to generate better error messages.

def f[x, y, z] {
  return x
}

a = read[]
x = range[1, 10].map[x. tuple[1, 5, x, 'hi']].filter[t. t[2] == 5]
y = 

# Maybe, limit the language so that making programs in CPL massively
# parallel would be easy too.

"""

class Ast(object):
  pass
