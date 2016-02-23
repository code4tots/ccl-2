package com.ccl.core;

public class RuntimeWithStdlib extends Runtime {
  public void populateGlobalScope(Scope scope) {
    super.populateGlobalScope(scope);
    scope
        .put("BuiltinFunction", BuiltinFunction.META)
        .put("import", new BuiltinFunction("import") {
          @Override
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 1);
            return importModule(args.get(0).as(Text.class).getValue());
          }
        });
  }
}
