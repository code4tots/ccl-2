package com.chatty;

import java.util.ArrayList;

public abstract class Value {
  public abstract Blob getMeta();
  public final Value get(String key) {
    return getMeta().getattr(key).bind(this);
  }
  public Value bind(Value owner) { return this; }
  public <T> T as(Class<T> cls) {
    try {
      return cls.cast(this);
    } catch (ClassCastException e) {
      throw new Err(
          "Expected " + cls.getName() + " but found " + getClass().getName());
    }
  }
}
