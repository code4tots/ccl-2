package com.chatty;

import java.util.HashMap;

public final class Blob extends Value {
  public static final Blob META = new Blob();

  private final Blob meta;
  private final HashMap<String, Value> table;
  public Blob(Blob meta, HashMap<String, Value> table) {
    this.meta = meta;
    this.table = table;
  }
  public Blob(Blob meta) {
    this(meta, new HashMap<String, Value>());
  }
  private Blob() {
    this.meta = this;
    this.table = new HashMap<String, Value>();
  }
  public Blob getMeta() { return meta; }
  public Value getattr(String key) {
    Value value = table.get(key);
    if (value == null)
      throw new Err("Attribute " + key + " not found");
    return value;
  }
  public Blob setattr(String key, Value value) {
    table.put(key, value);
    return this;
  }
}
