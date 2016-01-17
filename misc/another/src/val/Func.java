package com.ccl.val;

public abstract class Func extends Val {
  public final static Meta META = new Meta("Func");
  public final Meta getMeta() { return META; }
  public abstract Val call(Val self, List args);
  public final int hashCode() { return identityHash(); }
}
