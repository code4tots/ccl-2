package com.ccl;

import java.util.HashMap;

public final class Scope {
  private final Scope parent;
  private final HashMap<String, Value> table = new HashMap<String, Value>();
  public Scope(Scope parent) { this.parent = parent; }
  public Scope put(String key, Value value) {
    table.put(key, value);
    return this;
  }
  public Value getOrNull(String key) {
    Value value = table.get(key);
    if (value != null)
      return value;
    if (parent != null)
      return parent.getOrNull(key);
    return null;
  }
  public Value get(String key) {
    Value value = getOrNull(key);
    if (value == null)
      throw new Err("No such key: " + key);
    return value;
  }
}
