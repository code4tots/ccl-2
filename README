Sanity (CCL - Cackle)
=====================

Simple programming language with an object model a lot like Lua's.

Add 'exec/unix' (for Mac/Linux etc) or 'exec/windows' (for Windows) to the path.

Usage: xccl <path_to_ccl_script>

If you've run a script at least once with xccl, you can just use

Usage: ccl  <path_to_ccl_script>

xccl is like ccl, except it recompiles the Java binaries before running.

Design Philosophy
-----------------

  * Design is easy, but good design is hard.
    * Try to refrain from coming up with radically new language designs.
      * I will on ocassion break this guideline when I have a feature idea
        for which I think I understand the trade offs and consequences
        well enough. (e.g. function call syntax)
    * Cherry pick features that I understand well from other languages.
      * Pick out good features from other languages, but also
      * pick features such that collectively they play well with each other.

Goals
-----

  * Easy to implement and port to new platforms. This is accomplished by:
    * using a very easy to parse syntax.
    * very simple and flexible object model.
    * pushing a lot of functionality into core libraries written in CCL.

  * Easy desktop gui for
    * making simple games
    * making simple utilities
    * designing a text editor

  * Easy mobile gui (android/iOS) for
    * making simple games really quick
    * making simple apps and bodges

    * could probably port to iOS using Google's j2objc or something.

  * Eventually, make the implementation fast enough for contests.


TODO
----

  * Implement some sort of speed performance profiling.
    * At function level -- so CCL users can see which
      functions they wrote that are taking up a lot of time
    * At 'Ast' level -- so I can get a better sense of
      what is slow and what I might be able to speed up.

    * These performance profiling probably don't have to be
      too sophisticated. Just simple timing at entrance
      and exit for function level should be good enough.
      This is going to be a bit weird for functions that
      recurse, but I think for the most part the data
      will still be useful.


Directory layout
----------------

TODO: Elaborate here.

src/
  Where all the Java code for the interpreter is.

cls/
  Generated Java class files.

mods/
  Sanity library modules.

misc/
  Miscellaneous things that aren't actually part of running CCL programs.

  experiments/
    Various things I've experimented with that aren't necessarily included in
    the language proper.

  fbhc/
    Solve facebook hackercup problems to test performance and language
    usability.


Design
------

Tier N can compile with only the classes from Tier 1 ... N as dependencies.

1. err
2. grammar
3. object
4. runtime
