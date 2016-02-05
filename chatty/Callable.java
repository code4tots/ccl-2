package com.chatty;

public abstract class Callable extends Value {
  public abstract Value call(List args);
}
