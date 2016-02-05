package com.chatty;

import java.util.ArrayList;

public abstract class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }
  public abstract Value eval(Scope scope);

  public static final class Call extends Ast {
    public final Ast f;
    public final ArrayList<Ast> args;
    public final Ast vararg; // nullable
    public Call(Token token, Ast f, ArrayList<Ast> args, Ast vararg) {
      super(token);
      this.f = f;
      this.args = args;
      this.vararg = vararg;
      if (vararg != null)
        throw new Err("vararg not yet supported");
    }
    public Value eval(Scope scope) {
      return f.eval(scope).as(Callable.class).call(scope, args);
    }
  }

  public static final class Number extends Ast {
    public final Double value;
    public Number(Token token, Double value) {
      super(token);
      this.value = value;
    }
    public Value eval(Scope scope) {
      return new com.chatty.Number(value);
    }
  }

  public static final class Text extends Ast {
    public final String value;
    public Text(Token token, String value) {
      super(token);
      this.value = value;
    }
    public Value eval(Scope scope) {
      return new com.chatty.Text(value);
    }
  }

  public static final class Name extends Ast {
    public final String value;
    public Name(Token token, String value) {
      super(token);
      this.value = value;
    }
    public Value eval(Scope scope) {
      return scope.get(value);
    }
  }
}
