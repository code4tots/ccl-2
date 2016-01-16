package com.ccl.val;

import java.util.Iterator;

public final class BuiltinIter extends Val {
  public BuiltinIter from(Iterator<Val> val) { return new BuiltinIter(val); }
  public static final Meta META = new Meta("BuiltinIter");
  private final Iterator<Val> val;
  private BuiltinIter(Iterator<Val> val) { this.val = val; }
  public Iterator<Val> iterator() { return val; }
  public int hashCode() { return val.hashCode(); }
  public Meta getMeta() { return META; }
}
