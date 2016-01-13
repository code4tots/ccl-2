package com.ccl.val;

public final class Str extends Val {
  public static Str from(String val) { return new Str(val); }
  public static final Meta META = new Meta("Str");
  private final String val;
  private Str(String val) { this.val = val; }
  public Meta getMeta() { return META; }
  public int hashCode() { return val.hashCode(); }
}
