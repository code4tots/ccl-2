About Dream
===========

The contents of the directory 'contents' contains work on a really cool idea I have for a language that would be (1) simple, (2) fast (semantically equivalent to C/C++), and (3) pretty expressive (way more expressive than C/C++), but I'm afraid that it might be alot of work, and I'm not sure what the limits of its expressiveness are.

The idea is to have a language that is basically C, strip away basically all the bitwise nonsense (i.e. no poniter arithmetic, etc), and add templates (kind of like in C++) with type pattern matching.

Basically the only top level constructs are function templates and class templates (no static members or methods). No inheritance, and no runtime polymorphism.

Sample code:

    # RepeatList is a function with a single argument 'list'
    # with type pattern 'List[?T]'. This function will match
    # arguments of types List[Int] or List[String], and will
    # replace all future instances of 'T' in the function with
    # 'Int' or 'String' as the case may be.

    RepeatList[list List[?T]] List[T] {
      let newList = init[new List[T]]
      let i = 0
      while i < Size[list] {
        Append[newList, Get[list, i]]
      }
      while i < Size[list] {
        Append[newList, Get[list, i]]
      }
      return newList
    }

    Main[] {
      # List literals must have at least one element so that
      # their type may be deduced.
      # Otherwise, use e.g. 'init[new List[T]]'
      let strings = %["a", "b", "c"]

      Print[RepeatList[strings]] # %["a", "b", "c", "a", "b", "c"]

      Print[RepeatList[%[1, 2]]] # %[1, 2, 1, 2]
    }

----

    # A slightly more interesting example on how 'Map', 'Filter',
    # or 'Reduce' might be implemented.

    # It is a bit tacky that I have to create a separate class
    # just for creating anonymous functions.
    # In the future, I might think of adding syntactic sugar for
    # anonymous functions, however, as it is here, each anonymous
    # function will still be its own type. So technically, function
    # templates will be accept an extra dummy argument that really
    # takes no value (i.e. instances of Fn contain no value -- we
    # really only care about type information). However, this is
    # something the translator/compiler should be able to easily
    # optimize away.
    class Fn {}
    Apply[f Fn, Int x] Int {
      return Add[x, 1]
    }

    # Of course, you could also have a parallel implementation.
    Map[f ?_, list List[?T]] List[T] {
      let newList = init[new List[T]]
      let i = 0
      while i < Size[list]
        Append[newList, Apply[f, Get[list, i]]]
      return newList
    }

----

    # What about registering function handles for GUIs you might ask?
    # Well they can all be predetermined at compile time

    class MyButtonHandler {}
    HandleClick[h MyButtonHandler, event ClickDownEvent] {
      Print["Button was clicked!"]
    }

    Main[] {
      MakeButton[new MyButtonHandler, "Some text"]
    }

    # A major philosophy it seems is that there is no dispatch going on
    # 'behind your back'. Whenever there is branching going on, you
    # specify it explicitly.
