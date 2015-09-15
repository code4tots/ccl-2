All interaction with the outside world must go through the 'universe' object in some way.

When a program is started, the target class is instantiated, and the 'Run' method is called with a single 'universe' argument.

It may not be possible to construct a universe object normally from within the language.

In order to implement e.g. the singleton pattern, you must add an object to the registry in the universe object.
A unique identifier associated with a given class (yet to be implemented) may be useful for this purpose.

Classes required of target language

  Object
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool
    And(Object) : Bool
    Or(Object) : Bool

  Universe
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String

  Nil
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool

  Bool
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool

  Number
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool
    Add(String)

  String
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool
    Add(String)

  List
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool

  Set
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool

  Map
    Equal(Object) : Bool
    ToString() : String
    Inspect() : String
    ToBool() : Bool
