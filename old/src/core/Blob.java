package com.ccl.core;

import java.util.HashMap;
import java.util.Iterator;

public final class Blob extends Value {

  // The one META to rule them all.
  public static final Blob META = new Blob();

  // We need to first assign something to 'META' before
  // referencing e.g. 'Text'. Since initializing 'Text' requires
  // that 'Blob.META' already be set.
  static {
    META
        .setattr("extends", new BuiltinFunction("Meta@extends") {
          @Override
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 1);
            HashMap<String, Value> c = owner.as(Blob.class).getValue();
            HashMap<String, Value> d = args.get(0).as(Blob.class).getValue();
            Iterator<String> keys = d.keySet().iterator();
            while (keys.hasNext()) {
              String key = keys.next();
              if (c.get(key) == null) {
                c.put(key, d.get(key));
              }
            }
            return owner;
          }
        });
  }

  public static final Blob MODULE_META = new Blob(META);

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
    if (meta == null) {
      throw new Err("Invalid meta passed to Blob (null)");
    }
    this.meta = meta;
    this.attrs = attrs;
  }

  @Override
  public Blob getMeta() {
    return meta;
  }

  protected HashMap<String, Value> getValue() {
    return attrs;
  }

  public Blob setattr(String key, Value value) {
    attrs.put(key, value);
    return this;
  }

  public Value getattr(String key) {
    Value value = attrs.get(key);
    if (value == null) {
      Value attr = attrs.get("name");
      if (attr == null) {
        throw new Err("No such attribute '" + key + "' for unnamed Blob");
      } else {
        String name = attr.as(Text.class).getValue();
        throw new Err("No such attribute '" + key + "' for " + name);
      }
    }
    return value;
  }

  public boolean hasattr(String key) {
    return attrs.get(key) != null;
  }
}
