package com.ccl.core;

import java.util.HashMap;

public class Runtime {
  private Scope global = new Scope(null);

  protected HashMap<String, Blob> moduleRegistry =
      new HashMap<String, Blob>();

  public Runtime() {
    populateGlobalScope(global);
  }

  public void populateGlobalScope(Scope scope) {
    scope
        .put("nil", Nil.value)
        .put("true", Bool.yes)
        .put("false", Bool.no)
        .put("Value", Value.META)
        .put("Meta", Blob.META)
        .put("Module", Blob.MODULE_META)
        .put("Bool", Bool.META)
        .put("List", List.META)
        .put("Nil", Nil.META)
        .put("Number", Number.META)
        .put("Text", Text.META)
        .put("UserFunction", UserFunction.META)
        .put("BuiltinFunction", BuiltinFunction.META)
        .put("import", new BuiltinFunction("import") {
          @Override
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 1);
            return importModule(args.get(0).as(Text.class).getValue());
          }
        })
        .put("L", new BuiltinFunction("L") {
          @Override
          public Value calli(Value owner, List args) {
            return args;
          }
        })
        .put("err", new BuiltinFunction("err") {
          @Override
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 1);
            throw new Err(args.get(0).toString());
          }
        })
        .put("global", new Blob(
            new Blob(Blob.META)
                .setattr("name", Text.from("Global"))
                .setattr("__call__", new BuiltinFunction("Global#__call__") {
                  @Override
                  public Value calli(Value owner, List args) {
                    ErrUtils.expectArglen(args, 1);
                    return global.get(args.get(0).as(Text.class).getValue());
                  }
                })
                .setattr(
                    "__setitem__",
                    new BuiltinFunction("Global#__setitem__") {
                  @Override
                  public Value calli(Value owner, List args) {
                    ErrUtils.expectArglen(args, 2);
                    global.put(
                        args.get(0).as(Text.class).getValue(),
                        args.get(1));
                    return args.get(1);
                  }
                })
        ));
  }

  public final Scope getGlobalScope() {
    return global;
  }

  // By overriding 'makeEvaluator', subclasses can decide to use a different
  // Evaluator than the default one provided in this package.
  public Evaluator makeEvaluator(Scope scope) {
    return new Evaluator(this, scope);
  }

  public final void runMainModule(String code) {
    runModule(new Parser(code, "<main>").parse(), "__main__");
  }

  public final Blob runModule(Ast.Module module, String name) {
    Scope scope = new Scope(global);
    scope.put("__name__", Text.from(name));
    Evaluator evaluator = makeEvaluator(scope);
    evaluator.visit(module);
    return new Blob(Blob.MODULE_META, scope.table);
  }

  public final Blob loadModule(String uri) {
    return runModule(new Parser(readModule(uri), uri).parse(), uri);
  }

  public final Blob importModule(String uri) {
    if (moduleRegistry.get(uri) == null) {
      moduleRegistry.put(uri, loadModule(uri));
    }
    return moduleRegistry.get(uri);
  }

  // In order to import modules, subclass Runtime must override
  // 'readModule'.
  public String readModule(String uri) {
    throw new Err("Importing module not supported");
  }
}
