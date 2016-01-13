package com.ccl.val;

import com.ccl.err.Err;
import java.util.HashMap;
import java.util.Iterator;

public final class Map extends Val {
  public static Map from(HashMap<Val, Val> v) { return new Map(v); }
  public static Map from(Val... kvs) {
    if (kvs.length%2 != 0)
      throw new Err("Map requires an even number of arguments");
    HashMap<Val, Val> hm = new HashMap<Val, Val>();
    for (int i = 0; i < kvs.length; i += 2)
      hm.put(kvs[i], kvs[i+1]);
    return new Map(hm);
  }
  public static final Meta META = new Meta("Map");
  private final HashMap<Val, Val> val;
  private Map(HashMap<Val, Val> val) { this.val = val; }
  public Meta getMeta() { return META; }
  public int hashCode() { return val.hashCode(); }
  public Iterator<Val> iterator() { return val.keySet().iterator(); }
}
