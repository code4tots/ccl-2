package com.chatty;

import java.util.ArrayList;

public abstract class Value {
  public Value call(List args) { throw new Err("call not supported"); }
  public Value bind(Value owner) { return this; }
  public <T> T as(Class<T> cls) { return cls.cast(this); }
}
