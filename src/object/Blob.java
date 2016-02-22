package com.ccl.core;

import java.util.HashMap;

public final class Blob extends Value {

  // The one META to rule them all.
  public static final Blob META = new Blob()
      .setattr("name", Text.from("Meta"));

  private final Blob meta;

  private final HashMap<String, Value> attrs;

  // This constructor is meant to be used only for the construction of the
  // one META to rule them all.
  private Blob() {
    this.meta = this;
    this.attrs = new HashMap<String, Value>();
  }

  public Blob(Blob meta) {
    this(meta, new HashMap<String, Value>());
  }

  public Blob(Blob meta, HashMap<String, Value> attrs) {
    this.meta = meta;
    this.attrs = attrs;
  }

  @Override
  public Blob getMeta() {
    return meta;
  }

  public Blob setattr(String key, Value value) {
    attrs.put(key, value);
    return this;
  }

  public Value getattr(String key) {
    Value value = attrs.get(key);
    if (value == null) {
      throw new Err("No such attribute '" + key + "'");
    }
    return value;
  }

  public boolean hasattr(String key) {
    return attrs.get(key) != null;
  }
}
