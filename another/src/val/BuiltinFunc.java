package com.ccl.val;

import com.ccl.err.Err;

public abstract class BuiltinFunc extends Func {
  public final String name;
  public BuiltinFunc(String name) { this.name = name; }
  public final Val call(Val self, List args) {
    Val val = calli(self, args);
    if (val == null)
      throw new Err("Builtin function " + name + " returned null!");
    return val;
  }
  protected abstract Val calli(Val self, List args);
}
