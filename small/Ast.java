package com.ccl.core;

public abstract class Ast {

  public final Token token;
  public Ast(Token token) { this.token = token; }

  public abstract static class Statement extends Ast {
    public abstract void run(Context ctx);
  }

  public abstract static class Expression extends Ast {
    public abstract Value eval(Scope scope);
  }

  public abstract static class Pattern extends Ast {
    public abstract void assign(Scope scope, Value value);
  }
}
