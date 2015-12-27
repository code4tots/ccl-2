
=======================================

# What about anonymous functions?
# Each function instance could be its own type.
# This way, anything inside an anonymous function will be inlined.

Main[] Void {
  Print[Map[$[x Int]Int Add[x, 1], %[1, 2, 3]]] # %[2, 3, 4]
}


# Of course, the above would really just be syntactic sugar.
# The above behavior can be emulated like so:

class AnonymousFunctionMarker {}

Apply[f AnonymousFunctionMarker, Int x] Int {
}

# We *could* also speed things up with parallel computing.

Map[f ?_, xs List[?T]] List[T] {
  let len = Len[ys]
  let ys = Init[new List[?T], len]
  let i = 0
  let f = new AnonymousFunctionMarker
  while Lt[i, len] {
    Set[ys, i, Apply[f, Get[xs, i]]]
    i = Add[i, 1]
  }
  return ys
}
