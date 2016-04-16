package com.ccl.core;

import java.util.HashMap;
import java.util.Iterator;

public final class Map extends Value {
  
  public static final Blob META = new Blob(Blob.META)
      .setattr("repr", new BuiltinFunction("Map@repr") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 0);
          StringBuilder sb = new StringBuilder("M[");
          HashMap<Value, Value> map = owner.as(Map.class).getValue();
          Iterator<Value> it = map.keySet().iterator();
          boolean first = true;
          while (it.hasNext()) {
            if (!first)
              sb.append(", ");
            Value key = it.next();
            sb.append(key.toString());
            sb.append(", ");
            sb.append(map.get(key));
            first = false;
          }
          sb.append("]");
          return Text.from(sb.toString());
        }
      })
      .setattr("__eq__", new BuiltinFunction("Text@__eq__") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 1);
          if (args.get(0) instanceof Map)
            return owner.as(Map.class).value.equals(
                ((Map) args.get(0)).value) ? Bool.yes : Bool.no;
          return Bool.no;
        }
      })
      .setattr("has", new BuiltinFunction("Text@has") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 1);
          return owner.as(Map.class).value.get(args.get(0)) != null ?
              Bool.yes : Bool.no;
        }
      })
      .setattr("rm", new BuiltinFunction("Text@rm") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 1);
          owner.as(Map.class).value.remove(args.get(0));
          return Nil.value;
        }
      });

  @Override
  public Blob getMeta() {
    return META;
  }

  public static Map from(HashMap<Value, Value> value) {
    return new Map(value);
  }

  private final HashMap<Value, Value> value;

  private Map(HashMap<Value, Value> value) {
    this.value = value;
  }

  protected HashMap<Value, Value> getValue() {
    return value;
  }

  public Value get(Value i) {
    return value.get(i);
  }

  public Map put(Value key, Value value) {
    this.value.put(key, value);
    return this;
  }

  public int size() {
    return value.size();
  }
}
