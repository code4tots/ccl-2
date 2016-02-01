package com.ccl;

public final class Context {
  public final Scope scope;
  public Value value = Value.nil;
  public Ast ast = null;
  public Context(Scope scope) { this.scope = scope; }
}
