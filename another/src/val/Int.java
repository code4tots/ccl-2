package com.ccl.val;

import java.math.BigInteger;

public final class Int extends Val {
  public static Int from(int i) { return new Int(BigInteger.valueOf(i)); }
  public static Int from(long i) { return new Int(BigInteger.valueOf(i)); }
  public static final Meta META = new Meta("Int");
  private final BigInteger val;
  public Meta getMeta() { return META; }
  private Int(BigInteger val) { this.val = val; }
  public int hashCode() { return val.hashCode(); }
  public int asIndex() { return val.intValue(); }
}
