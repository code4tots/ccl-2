package com.ccl.core;

public final class UserFunction extends Function {

  public static final Blob META = new Blob(Blob.META);

  private final Ast.Function ast;
  private final Scope scope;
  private final Runtime runtime;

  public UserFunction(Ast.Function ast, Scope scope, Runtime runtime) {
    this.ast = ast;
    this.scope = scope;
    this.runtime = runtime;
  }

  @Override
  public Blob getMeta() {
    return META;
  }

  @Override
  public Value call(Value owner, List args) {
    Scope scope = this.scope;
    if (ast.newScope) {
      scope = new Scope(scope);
      scope.put("self", this);
    }
    new Assigner(scope).visit(ast.args, args);
    Value result = runtime.makeEvaluator(scope).visit(ast.body);
    return result == null ? Nil.value : result;
  }
}
