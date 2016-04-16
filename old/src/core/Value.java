package com.ccl.core;

public abstract class Value {

  public static final Blob META = new Blob(Blob.META);

  public abstract Blob getMeta();

  public final <T extends Value> T as(Class<T> cls) {
    try {
      return cls.cast(this);
    } catch (ClassCastException e) {
      throw new Err(
          "Expected result to be " + cls.getName() +
          " but found " + getClass().getName());
    }
  }

  public final Value callf(List args) {
    if (this instanceof Function) {
      return ((Function) this).call(this, args);
    } else {
      return this.call("__call__", args);
    }
  }

  public final Value call(String name, List args) {
    Blob meta = getMeta();
    if (meta == null) {
      throw new Err("WTF: " + getClass().getName());
    }
    Value attr = meta.getattr(name);
    Function func = attr.as(Function.class);
    return func.call(this, args);
  }

  public final Value callx(String name, Value... args) {
    return call(name, List.from(args));
  }

  // Convenience methods.
  public final boolean equals(Value value) {
    return callx("__eq__", value).truthy();
  }

  public final boolean truthy() {
    return callx("__bool__").as(Bool.class).getValue();
  }

  // Java Object overrides.
  @Override
  public final boolean equals(Object value) {
    return value instanceof Value && equals((Value) value);
  }

  @Override
  public final String toString() {
    return callx("str").as(Text.class).getValue();
  }

  @Override
  public final int hashCode() {
    return (int) callx("hash").as(Number.class).getValue();
  }
}
