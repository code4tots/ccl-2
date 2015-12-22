Roadmap -- List of features I'm thinking of adding
==================================================

Features under consideration
----------------------------

* Some form of context manager (like 'with' in Python).
* Anonymous classes (like in Java).
* User definable constructors (like '__new__' in Python).
  * However, unlike '__new__' in Python, constructors should not be
    inheritable.
    Instead, '__init__' or some variant should be used for inheritance
    initialization, the constructor is really just an arbitrary function
    attached to a type for syntactic sugar.
* Anonymous functions.
* Speed. CCL as currently implemented is ridiculously slow.
  * Maybe address this by converting Ast within functions into bytecode,
    kind of like the way Python does it.
  * More importantly, figure out where the bottleneck in the processing is.
    Don't fall into premature optimization.
* Trace object. Right now 'trace[]' returns a String.
  Return a Trace object instead.
* Desktop GUI API
* Desktop GUI API with OpenGL
* Android API
* iOS API (probably using some translation tool like Google's j2objc)

Done features
-------------

* Separate syntax for calling methods as opposed to retrieving attributes.
  * I'm considering leaving '.' for methods and using '@' for attributes.
* Calling super methods.
