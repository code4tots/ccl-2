Roadmap -- List of features I'm thinking of adding
==================================================

Features under consideration
----------------------------

* Some form of context manager (like 'with' in Python).
* Speed. Simple implementation is currently ridiculously slow.
  * Maybe address this by converting Ast within functions into bytecode,
    kind of like the way Python does it.
    Traversing the AST cost more than bytecode because:
      1. calling recursively, and
      2. virtual method dispatch.
      3. ???
    Switching on integer opcodes is probably faster than calling
    virtual methods.
  * It might be easier to write a profiler if the interpreter just naively
    runs over the AST.
  * More importantly, figure out where the bottleneck in the processing is.
    Don't fall into premature optimization.
* Desktop GUI API
* Desktop GUI API with OpenGL
* Android API
* iOS API (probably using some translation tool like Google's j2objc)
* Add ability to easily add native modules.
* Iterators
  * You can iterate over lists with numeric index, and iterate over
    maps by getting a list of keys. However, iterators *are* more
    convenient.
    I still want to think more about the consequences though,
    and whether I can be more elegant.
    Java and Python seem to be pretty straight forward about this,
    but Ruby is another interesting way to do it.
* Python style for loops (if I decide to add iterators).
  * Another option is to follow Ruby's style and pass some sort of block.
    I'm rather conflicted about this -- allowing more mechanisms to pass
    around thunks of code is a very powerful thing and could get really dirty
    really fast.
    Technically, lambdas already are this mechanism, but with just lambdas
    and without iterators, this seems rather clunky.

Done features
-------------

* Separate syntax for calling methods as opposed to retrieving attributes.
  * I'm considering leaving '.' for methods and using '@' for attributes.
