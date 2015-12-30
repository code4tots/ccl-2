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
* Generators/Coroutines
  Maybe. I want it to be really elegant...
* Channel/Goroutines
  Maybe instead of iterators/generators, I could take a page from golang and
  use channels and goroutines.

  At first I was concerned that channels wouldn't blocked, and in
  situation like with iterators, you actually want the iterator to block,
  and wait until caller asks for more.

  By default, in Go, apparently channels block unless you specify specify
  a buffer size: https://www.golang-book.com/books/intro/10

* __new__ attribute convention, so that 'new' calls '__new__' if a type has
  a '__new__' attribute?
  * Hmm, wait, then how do we allocate a Blob from inside of 'MyType.__new__'?
    If you tried to call 'new[MyType]', it would recurse.

* There are many interesting cases to consider on what to do about
  coroutine/iterators.

  For instance, sometimes you want 'List@map' to run a function in parallel on
  all the items on the list.

  On the other hand, if you are iterating over it, sometimes you want
  'List@map' to be lazy and not necessarily process the entire list -- we only
  want values up to what we have already specified.

  How are we going to indicate when we want 'map' to be strict or lazy?

  What about with an 'each' method, where 'each' is like 'map' but doesn't
  save the values? We might want this one to be always strict, since
  what is the point if not to use it as a looping construct? This is basically
  a lazy 'map' where we request all values but drop them immediately, right?
  What about concurrency, do we want 'each' to specify parallelism?
  Is it useful to have an 'each' method that runs in parallel? Would 'each'
  still be useful even if you couldn't mutate the outside world from inside
  the callback?

  Fold/reduce at least may be easier? Foldl/foldr we always want sequential
  since ordering is specified and we always want strict since in there is no
  partial results to return as in lists -- just the final combination.
  Reduce we always want to indicate concurrency I would imagine.


Done features
-------------

* Separate syntax for calling methods as opposed to retrieving attributes.
  * I'm considering leaving '.' for methods and using '@' for attributes.
