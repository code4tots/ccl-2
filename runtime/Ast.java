package com.ccl.runtime;

import java.util.ArrayList;
import java.math.BigInteger;

public abstract class Ast {

public abstract static class Pattern extends Ast {}
public static final class List extends Pattern {
  public final ArrayList<Pattern> args;
  public final ArrayList<Pattern> optargs;
  public final String vararg; // nullable
  public List(
      ArrayList<Pattern> args,
      ArrayList<Pattern> optargs,
      String vararg) {
    this.args = args;
    this.optargs = optargs;
    this.vararg = vararg;
  }
}
public static final class Name extends Pattern {
  public final String name;
  public Name(String name) { this.name = name; }
}

public abstract static class Expression extends Ast {}
public static final class Int extends Expression {
  public final BigInteger value;
  public Int(BigInteger value) { this.value = value; }
}
public static final class Float extends Expression {
  public final Double value;
  public Float(Double value) { this.value = value; }
}
public static final class Str extends Expression {
  public final String value;
  public Str(String value) { this.value = value; }
}
public static final class Func extends Expression {
  public final boolean scope;
  public final Pattern args;
  public final Statement body;
  public Func(boolean scope, Pattern args, Statement body) {
    this.scope = scope;
    this.args = args;
    this.body = body;
  }
}
public static final class Find extends Expression {
  public final String name, message;
  public Find(String name, String message) {
    this.name = name;
    this.message = message;
  }
}
public static final class Callm extends Expression {
  public final String name;
  public final ArrayList<Expression> args;
  public final Expression vararg; // nullable
  public final String message;
  public Callm(
      String name, ArrayList<Expression> args, Expression vararg,
      String message) {
    this.name = name;
    this.args = args;
    this.vararg = vararg;
    this.message = message;
  }
}
public static final class Assign extends Expression {
  public final Pattern pattern;
  public final Expression value;
  public final String message;
  public Assign(Pattern pattern, Expression value, String message) {
    this.pattern = pattern;
    this.value =  value;
    this.message = message;
  }
}
public static final class Ife extends Expression {
  public final Expression cond, body, other;
  public Ife(Expression cond, Expression body, Expression other) {
    this.cond = cond;
    this.body = body;
    this.other = other;
  }
}
public static final class Or extends Expression {
  public final Expression left, right;
  public Or(Expression left, Expression right) {
    this.left = left;
    this.right = right;
  }
}
public static final class And extends Expression {
  public final Expression left, right;
  public And(Expression left, Expression right) {
    this.left = left;
    this.right = right;
  }
}
public static final class Import extends Expression {
  public final String name, message;
  public Import(String name, String message) {
    this.name = name;
    this.message = message;
  }
}

public abstract static class Statement extends Ast {}

}
