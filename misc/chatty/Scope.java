package com.chatty;

import java.util.HashMap;

public final class Scope {
  public final HashMap<String, Value> table;
  public final Scope parent;
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Value>();
  }
  public Value getOrNull(String name) {
    Value value = table.get(name);
    if (value == null && parent != null)
      return parent.getOrNull(name);
    return value;
  }
  public Value get(String name) {
    Value value = getOrNull(name);
    if (value == null)
      throw new Err("Variable " + name + " not found");
    return value;
  }
  public Scope put(String name, Value value) {
    table.put(name, value);
    return this;
  }
}
