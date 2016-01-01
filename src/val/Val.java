import java.util.ArrayList;
import java.util.HashMap;

public abstract class Val {

  public static final HashMap<String, Val> MMMeta = new Hmb()
      .put("name", Str.from("Meta"))
      .hm;

  public static final HashMap<String, Val> MMVal = new Hmb()
      .put("name", Str.from("Val"))
      .hm;

  public abstract HashMap<String, Val> getMeta();
  public final String getMetaName() {
    Val name = getMeta().get("name");
    return name == null ? "*unnamed*" : name.as(Str.class, "name").val;
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
          "Expected '" + name + "' to be " + getClass().getName() +
          " but found " + cls.getName());
    }
  }
  public final boolean equals(Object other) {
    return (other instanceof Val) && equals((Val) other);
  }
  public final boolean equals(Val other) {
    return call("__eq__", other).truthy();
  }
  public final String toString() {
    return call("__str__").as(Str.class, "result of __str__").val;
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
      return put(bf.name, bf);
    }
  }

  public static ArrayList<Val> toArrayList(Val... args) {
    ArrayList<Val> al = new ArrayList<Val>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }

}
