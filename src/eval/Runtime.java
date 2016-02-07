package com.ccl.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class Runtime {
  protected Scope global = new Scope(null);  
  private final HashMap<String, Blob> moduleRegistry =
      new HashMap<String, Blob>();

  protected Runtime() {
    populateGlobalScope(global);
  }

  protected Blob importModule(String uri) {
    if (moduleRegistry.get(uri) == null) {
      Scope scope = new Scope(global);
      scope.eval(loadModule(uri));
      moduleRegistry.put(
          uri,
          new Blob(Val.MMModule, scope.table));
    }
    return Err.notNull(moduleRegistry.get(uri));
  }

  protected abstract Ast.Module loadModule(String uri);

  protected void populateGlobalScope(Scope scope) {
    scope
        .put("nil", Nil.val)
        .put("true", Bool.tru)
        .put("false", Bool.fal)
        .put(Val.MMMeta)
        .put(Val.MMVal)
        .put(Nil.MM)
        .put(Bool.MM)
        .put(Num.MM)
        .put(Str.MM)
        .put(List.MM)
        .put(Map.MM)
        .put(Func.MM)
        .put(BuiltinIter.MM)
        .put(Regex.MM)
        .put(Channel.MM)
        .put(new BuiltinFunc("new") {
          public Val calli(Val self, ArrayList<Val> args) {
            Err.expectArglen(args, 1);
            return new Blob(args.get(0).as(Blob.class, "arg"));
          }
        })
        .put(new BuiltinFunc("L") {
          public Val calli(Val self, ArrayList<Val> args) {
            return List.from(args);
          }
        })
        .put(new BuiltinFunc("M") {
          public Val calli(Val self, ArrayList<Val> args) {
            if (args.size()%2 != 0)
              throw new Err(
                  "'M' requires an even number of arguments, but got " +
                  args.size());

            HashMap<Val, Val> attrs = new HashMap<Val, Val>();
            Iterator<Val> it = args.iterator();
            while (it.hasNext()) {
              Val key = it.next();
              Val val = it.next();
              attrs.put(key, val);
            }

            return Map.from(attrs);
          }
        })
        .put(new BuiltinFunc("err") {
          public Val calli(Val self, ArrayList<Val> args) {
            Err.expectArglen(args, 1);
            throw new Err(args.get(0)
                .call("str").as(Str.class, "result of method str").val);
          }
        })
        .put(new BuiltinFunc("go") {
          public Val calli(Val self, ArrayList<Val> args) {
            Err.expectArglen(args, 1);
            Evaluator.go(args.get(0));
            return Nil.val;
          }
        })
        // TODO: 'select'.
        // Expect an even number of arguments:
        // <channel>, <callable>, <channel>, <callable>, ...
        .put(new BuiltinFunc("sleep") {
          public Val calli(Val self, ArrayList<Val> args) {
            Err.expectArglen(args, 1);
            int millisec = args.get(0).as(Num.class, "arg").asIndex();
            try { Thread.sleep(millisec); }
            catch (InterruptedException e) { throw new Err(e); }
            return Nil.val;
          }
        })
        .put(new BuiltinFunc("time") {
          public Val calli(Val self, ArrayList<Val> args) {
            Err.expectArglen(args, 0);
            return Num.from(new java.util.Date().getTime());
          }
        })
        .put(new BuiltinFunc("import") {
          public Val calli(Val self, ArrayList<Val> args) {
            return importModule(args.get(0).as(
                Str.class, "module name argument").val);
          }
        })
        ;
    scope.put("GLOBAL", new Blob(new HashMap<String, Val>(), scope.table));
  }
}
