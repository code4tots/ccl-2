import java.util.ArrayList;
import java.util.HashMap;

public abstract class Val {

  public static final HashMap<String, Val> MMMeta = new Hmb()
      .put("name", Str.from("Meta"))
      .hm;

  public static final HashMap<String, Val> MMVal = new Hmb()
      .put("name", Str.from("Val"))
      .hm;

  public static final HashMap<String, Val> MMNil = new Hmb()
      .put("name", Str.from("Nil"))
      .hm;

  public static final HashMap<String, Val> MMBool = new Hmb()
      .put("name", Str.from("Bool"))
      .hm;

  public static final HashMap<String, Val> MMStr = new Hmb()
      .put("name", Str.from("Str"))
      .hm;

  public static final HashMap<String, Val> MMList = new Hmb()
      .put("name", Str.from("List"))
      .hm;

  public static final HashMap<String, Val> MMMap = new Hmb()
      .put("name", Str.from("Map"))
      .hm;

  public static final Nil nil = new Nil();
  public static final Bool tru = new Bool(true);
  public static final Bool fal = new Bool(false);

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
  public final boolean truthy() { return this != nil && this != fal; }
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

  public static final class Nil extends Val {
    public final HashMap<String, Val> getMeta() { return MMNil; }
  }

  public abstract static class Wrap<T> extends Val {
    public final T val;
    public Wrap(T t) { val = t; }
  }

  public static final class Bool extends Wrap<Boolean> {
    public Bool(Boolean val) { super(val); }
    public final HashMap<String, Val> getMeta() { return MMBool; }
  }

  public static final class Str extends Wrap<String> {
    public static Str from(String s) { return new Str(s); }
    public Str(String val) { super(val); }
    public final HashMap<String, Val> getMeta() { return MMStr; }
  }

  public static final class List extends Wrap<ArrayList<Val>> {
    public static List from(ArrayList<Val> s) { return new List(s); }
    public List(ArrayList<Val> val) { super(val); }
    public final HashMap<String, Val> getMeta() { return MMList; }
  }

  public static final class Map extends Wrap<HashMap<Val, Val>> {
    public static Map from(HashMap<Val, Val> s) { return new Map(s); }
    public Map(HashMap<Val, Val> val) { super(val); }
    public final HashMap<String, Val> getMeta() { return MMList; }
  }

  public static final class Blob extends Val {
    public final HashMap<String, Val> meta;
    public final HashMap<String, Val> attrs;
    public Blob(HashMap<String, Val> meta) {
      this(meta, new HashMap<String, Val>());
    }
    public Blob(HashMap<String, Val> meta, HashMap<String, Val> attrs) {
      this.meta = meta;
      this.attrs = attrs;
    }
    public final HashMap<String, Val> getMeta() { return MMList; }
  }

  // HashMap Builder
  protected static class Hmb {
    public final HashMap<String, Val> hm = new HashMap<String, Val>();
    public Hmb put(String name, Val value) {
      hm.put(name, value);
      return this;
    }
  }

  public static ArrayList<Val> toArrayList(Val... args) {
    ArrayList<Val> al = new ArrayList<Val>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }

}
