package com.ccl.core;

import java.util.HashMap;

public final class Nil extends Val {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Nil"))
      .hm;

  public static final Nil val = new Nil();

  private Nil() {}
  public final HashMap<String, Val> getMeta() { return MM; }
}
