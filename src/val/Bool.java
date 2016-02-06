package com.ccl.core;

import java.util.HashMap;
import java.util.ArrayList;

public final class Bool extends Val.Wrap<Boolean> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Bool"))
      .put(new BuiltinFunc("Bool#hash") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Num.from(self.as(Bool.class, "self").val.hashCode());
        }
      })
      .hm;

  public static final Bool tru = new Bool(true);
  public static final Bool fal = new Bool(false);

  private Bool(Boolean val) { super(val); }
  public static Bool from(Boolean val) { return val ? tru : fal; }
  public final HashMap<String, Val> getMeta() { return MM; }
}
