package com.ccl.val;

public final class Bool extends Val {
  public static final Meta META = new Meta("Bool");
  public static final Bool tru = new Bool();
  public static final Bool fal = new Bool();
  public Meta getMeta() { return META; }
  private Bool() {}
  public int hashCode() { return this == tru ? 1231 : 1237; }
}
