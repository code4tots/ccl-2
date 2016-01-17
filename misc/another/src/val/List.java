package com.ccl.val;

import java.util.ArrayList;

public final class List extends Val {
  public static final Meta META = new Meta("List");
  public static List from(ArrayList<Val> val) { return new List(val); }
  public static List from(Val... val) {
    ArrayList<Val> vals = new ArrayList<Val>();
    for (int i = 0; i < val.length; i++)
      vals.add(val[i]);
    return from(vals);
  }
  private final ArrayList<Val> val;
  private List(ArrayList<Val> val) { this.val = val; }
  public int hashCode() { return val.hashCode(); }
  public Meta getMeta() { return META; }
}
