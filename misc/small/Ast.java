package com.ccl.core;

public abstract class Ast {

  public final Token token;
  public Ast(Token token) { this.token = token; }

  public abstract static class Statement extends Ast {
    public Statement(Token token) { super(token); }
    public abstract void run(Context ctx);
  }

  public abstract static class Expression extends Ast {
    public Expression(Token token) { super(token); }
    public abstract Value eval(Scope scope);
  }

  public static final class Function extends Expression {
    public final boolean newScope;
    public final Pattern args;
    public final Statement body;
    public Function(
        Token token, boolean newScope, Pattern args, Statement body) {
      super(token);
      this.newScope = newScope;
      this.args = args;
      this.body = body;
    }
    public Value eval(Scope scope) {
      return new Value.Function(this, scope);
    }
  }

  public abstract static class Pattern extends Ast {
    public Pattern(Token token) { super(token); }
    public abstract void assign(Scope scope, Value value);
  }
}
