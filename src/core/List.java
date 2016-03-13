package com.ccl.core;

import java.util.ArrayList;
import java.util.Iterator;

public final class List extends Value {
  
  public static final Blob META = new Blob(Blob.META)
      .setattr("repr", new BuiltinFunction("List@repr") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 0);
          StringBuilder sb = new StringBuilder("L[");
          Iterator<Value> it = owner.as(List.class).getValue().iterator();
          if (it.hasNext()) {
            sb.append(it.next().toString());
            while (it.hasNext()) {
              sb.append(", ");
              sb.append(it.next().toString());
            }
          }
          sb.append("]");
          return Text.from(sb.toString());
        }
      })
      .setattr("__eq__", new BuiltinFunction("List@__eq__") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 1);
          ArrayList<Value> value = owner.as(List.class).getValue();
          Value other = args.get(0);
          if (other instanceof List) {
            return value.equals(((List) other).getValue()) ?
                Bool.yes : Bool.no;
          }
          return Bool.no;
        }
      });

  @Override
  public Blob getMeta() {
    return META;
  }

  public static List from(Value... value) {
    ArrayList<Value> list = new ArrayList<Value>();
    for (int i = 0; i < value.length; i++) {
      list.add(value[i]);
    }
    return new List(list);
  }

  public static List from(ArrayList<Value> value) {
    return new List(value);
  }

  private final ArrayList<Value> value;

  private List(ArrayList<Value> value) {
    this.value = value;
  }

  protected ArrayList<Value> getValue() {
    return value;
  }

  public Value get(int i) {
    return value.get(i);
  }

  public int size() {
    return value.size();
  }
}
