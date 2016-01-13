package com.ccl.val;

public final class Nil extends Val {
  public static final Meta META = new Meta("Nil");
  public static final Nil val = new Nil();
  public Meta getMeta() { return META; }
  private Nil() {}
  public int hashCode() { return identityHash(); }
}
