package com.ccl.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class Val {
  public static final HashMap<String, Val> MMMeta = new Hmb()
      .put("name", Str.from("Meta"))
      .put(new BuiltinFunc("Meta#extend") {
        public Val calli(Val self, ArrayList<Val> args) {
          HashMap<String, Val> m = self.as(Blob.class, "self").attrs;
          for (int i = 0; i < args.size(); i++) {
            HashMap<String, Val> h = args.get(i).as(Blob.class, "args").attrs;
            Iterator<java.util.Map.Entry<String, Val>> it = h.entrySet().iterator();
            while (it.hasNext()) {
              java.util.Map.Entry<String, Val> e = it.next();
              if (m.get(e.getKey()) == null) {
                m.put(e.getKey(), e.getValue());
              }
            }
          }
          return Nil.val;
        }
      })
      .put(new BuiltinFunc("Meta#repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          return Str.from(
              getMetaNameFromHashMap(self.as(Blob.class, "self").attrs));
        }
      })
      .hm;

  public static final HashMap<String, Val> MMVal = new Hmb()
      .put("name", Str.from("Val"))
      .put(new BuiltinFunc("Val#__meta__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          Val v = self.getMeta().get(args.get(0).as(Str.class, "arg").val);
          return v == null ? Nil.val : v;
        }
      })
      .put(new BuiltinFunc("Val#hash") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Num.from(System.identityHashCode(self));
        }
      })
      .hm;

  public static final HashMap<String, Val> MMModule = new Hmb()
      .put("name", Str.from("Module"))
      .hm;

  public abstract HashMap<String, Val> getMeta();
  public static final String getMetaNameFromHashMap(HashMap<String, Val> m) {
    Val name = m.get("name");
    return name == null ? "*unnamed*" : name.as(Str.class, "name").val;
  }
  public final String getMetaName() {
    return getMetaNameFromHashMap(getMeta());
  }
  public final Val call(String name, Val... args) {
    return call(name, toArrayList(args));
  }
  public final Val call(String name, ArrayList<Val> args) {
    Val method = getMeta().get(name);
    if (method == null)
      throw new Err(
          "No method named '" + name + "' for type " + getMetaName());
    return method.as(Func.class, name).call(this, args);
  }
  public final boolean truthy() { return this != Nil.val && this != Bool.fal; }
  public final <T extends Val> T as(Class<T> cls, String name) {
    try {
      return cls.cast(this);
    } catch (ClassCastException e) {
      throw new RuntimeException(
          "Expected '" + name + "' to be " + cls.getName() +
          " but found " + getClass().getName());
    }
  }
  public final boolean equals(Object other) {
    return (other instanceof Val) && equals((Val) other);
  }
  public final boolean equals(Val other) {
    return call("__eq__", other).truthy();
  }
  public final String toString() {
    return call("str").as(Str.class, "result of method str").val;
  }
  public final String repr() {
    return call("repr").as(Str.class, "result of method repr").val;
  }
  public final int hashCode() {
    return call("hash").as(Num.class, "result of method hash").asIndex();
  }

  public abstract static class Wrap<T> extends Val {
    public final T val;
    public Wrap(T t) { val = t; }
  }

  // HashMap Builder
  protected static class Hmb {
    public final HashMap<String, Val> hm = new HashMap<String, Val>();
    public Hmb put(String name, Val value) {
      hm.put(name, value);
      return this;
    }
    public Hmb put(BuiltinFunc bf) {
      String[] ss = bf.name.split("#");
      return put(ss[ss.length-1], bf);
    }
    public Hmb put(HashMap<String,Val> bf) {
      Val name = bf.get("name");
      if (name == null)
        throw new Err("Blob HashMap doesn't have a name!");
      String[] ss = name.as(Str.class, "FUBAR").val.split("#");
      return put(ss[ss.length-1], new Blob(Val.MMMeta, bf));
    }
  }

  public static ArrayList<Val> toArrayList(Val... args) {
    ArrayList<Val> al = new ArrayList<Val>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }

}
