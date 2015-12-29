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

Done features
-------------

* Separate syntax for calling methods as opposed to retrieving attributes.
  * I'm considering leaving '.' for methods and using '@' for attributes.
