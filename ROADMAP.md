Roadmap -- List of features I'm thinking of adding
==================================================

WIP
---

* Reduce the number of AST subclasses.
  * Do this by collapsing e.g. CallExpression into OperationExpression.
* Evaluate AST by using an explicit valueStack and operandStack.
  * This will make it easier to implement generators.
* Generators
* Desktop GUI API
* Desktop GUI API with OpenGL
* Android API
* iOS API (probably using some translation tool like Google's j2objc)
* Add ability to easily add native modules.

Done features
-------------

* Separate syntax for calling methods as opposed to retrieving attributes.
  * I'm considering leaving '.' for methods and using '@' for attributes.
