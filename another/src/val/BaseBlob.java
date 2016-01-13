package com.ccl.val;

import com.ccl.err.Err;
import java.util.HashMap;

/* package-private */ abstract class BaseBlob extends Val {
  private final HashMap<String, Val> attrs = new HashMap<String, Val>();

  public boolean has(String key) { return attrs.containsKey(key); }
  public Val get(String key) {
    Val val = attrs.get(key);
    if (val == null)
      throw new Err(
          "No attribute " + key +
          " for value of type " + getMetaName());
    return val;
  }
  public BaseBlob put(String key, Val val) {
    attrs.put(key, Err.notNull(val));
    return this;
  }
}
