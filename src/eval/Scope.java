package com.ccl.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class Scope {
  public final HashMap<String, Val> table;
  public final Scope parent;
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Val>();
  }
  public Val getOrNull(String name) {
    Val value = table.get(name);
    if (value == null && parent != null)
      return parent.getOrNull(name);
    return value;
  }
  public Scope put(String name, Val value) {
    table.put(name, value);
    return this;
  }
  public Scope put(BuiltinFunc bf) {
    return put(bf.name, bf);
  }
  public Scope put(HashMap<String,Val> bf) {
    Val name = bf.get("name");
    if (name == null)
      throw new Err("Blob HashMap doesn't have a name!");
    return put(name.as(Str.class, "FUBAR").val, new Blob(Val.MMMeta, bf));
  }
  /* package-private */ Scope put(Blob m) {
    return put(m.attrs.get("name").as(Str.class, "FUBAR").val, m);
  }
  public Val eval(Ast ast) {
    return new Evaluator(this).eval(ast);
  }
}
