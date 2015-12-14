Roadmap -- List of features I'm thinking of adding
==================================================

Features under consideration
----------------------------

* Some form of context manager (like 'with' in Python).
* Anonymous classes (like in Java).
* User definable constructors (like '__new__' in Python).
* Calling super methods.
* Anonymous functions.
* Speed. CCL as currently implemented is ridiculously slow.
* Trace object. Right now 'trace[]' returns a String.
  Return a Trace object instead.

Done features
-------------

* Separate syntax for calling methods as opposed to retrieving attributes.
  * I'm considering leaving '.' for methods and using '@' for attributes.
