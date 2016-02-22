package com.ccl.core;

import java.util.HashMap;

public class Runtime {
  private Scope global = new Scope(null);

  protected HashMap<String, Blob> moduleRegistry =
      new HashMap<String, Blob>();

  // META for module blobs.
  public static final Blob META = new Blob(Blob.META);

  public Runtime() {
    populateGlobalScope(global);
  }

  public void populateGlobalScope(Scope scope) {
    scope
        .put("nil", Nil.value)
        .put("true", Bool.yes)
        .put("false", Bool.no)
        .put("Meta", Blob.META)
        .put("Bool", Bool.META)
        .put("List", List.META)
        .put("Nil", Nil.META)
        .put("Number", Number.META)
        .put("Text", Text.META);
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
    runModule(new Parser(code, "<main>").parse());
  }

  public final Blob runModule(Ast.Module module) {
    Scope scope = new Scope(global);
    Evaluator evaluator = makeEvaluator(scope);
    evaluator.visit(module);
    return new Blob(META, scope.table);
  }

  public final Blob loadModule(String uri) {
    return runModule(new Parser(readModule(uri), uri).parse());
  }

  // In order to import modules, subclass Runtime must override 'readModule'.
  public String readModule(String uri) {
    throw new Err("Importing module not supported");
  }
}
