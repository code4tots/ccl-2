package com.ccl.core;

import java.util.ArrayList;

public abstract class Pattern {
  public abstract <T> void accept(PatternVisitor<T> visitor, T t);

  public static final class List extends Pattern {
    public final ArrayList<Pattern> args;
    public final ArrayList<Pattern> optargs;
    public final String vararg;
    public List(
        ArrayList<Pattern> args, ArrayList<Pattern> optargs, String vararg) {
      this.args = args;
      this.optargs = optargs;
      this.vararg = vararg;
    }
    public <T> void accept(PatternVisitor<T> visitor, T t) {
      visitor.visitList(this, t);
    }
  }
  public static final class Name extends Pattern {
    public final String name;
    public Name(String name) { this.name = name; }
    public <T> void accept(PatternVisitor<T> visitor, T t) {
      visitor.visitName(this, t);
    }
  }
}
