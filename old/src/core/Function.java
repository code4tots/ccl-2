package com.ccl.core;

public abstract class Function extends Value {
  public abstract Value call(Value owner, List args);
}
