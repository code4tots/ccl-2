package com.ccl.core;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class BuiltinFunc extends Func {
  public final String name;
  public BuiltinFunc(String name) { this.name = name; }
  public final String getTraceMessage() {
    return "\nin builtin function " + name;
  }
}
