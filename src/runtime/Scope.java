package com.ccl.core;

import java.util.HashMap;

public final class Scope {
  public final HashMap<String, Value> table;
  public final Scope parent;

  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Value>();
  }

  public boolean has(String name) {
    return table.get(name) != null || (parent != null && parent.has(name));
  }

  public Value get(String name) {
    Value value = table.get(name);
    if (value == null) {
      if (parent == null) {
          throw new Err("No variable named '" + name + "'");
      } else {
        return parent.get(name);
      }
    }
    return value;
  }

  public Scope put(String name, Value value) {
    table.put(name, value);
    return this;
  }
}
