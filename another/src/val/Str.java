package com.ccl.val;

public final class Str extends Val {
  public static final Meta META = new Meta("Str");
  private final String val;
  public Str(String val) { this.val = val; }
  public Meta getMeta() { return META; }
  public int hashCode() { return val.hashCode(); }
}
