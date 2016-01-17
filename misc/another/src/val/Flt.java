package com.ccl.val;

public final class Flt extends Val {
  public static Flt from(double i) { return new Flt(i); }
  public static final Meta META = new Meta("Float");
  private final double val;
  public Meta getMeta() { return META; }
  private Flt(double val) { this.val = val; }
  public int hashCode() {
    // Copied from source for java.lang.Double
    long v = Double.doubleToLongBits(val);
    return (int)(v^(v>>>32));
  }
}
