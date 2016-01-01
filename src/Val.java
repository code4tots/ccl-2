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

  public static final HashMap<String, Val> MMNum = new Hmb()
      .put("name", Str.from("Num"))
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

  public static final HashMap<String, Val> MMBuiltinFunc = new Hmb()
      .put("name", Str.from("BuiltinFunc"))
      .hm;

  public static final Nil nil = new Nil();
  public static final Bool tru = new Bool(true);
  public static final Bool fal = new Bool(false);

  public abstract HashMap<String, Val> getMeta();
  public final boolean truthy() { return this != nil && this != fal; }
  public final <T extends Val> T as(Class<T> cls) {
    try {
      return cls.cast(this);
    } catch (ClassCastException e) {
      throw new RuntimeException(
          "Expected " + getClass().getName() + " but found " + cls.getName());
    }
  }

  public static final class Nil extends Val {
    public final HashMap<String, Val> getMeta() { return MMNil; }
  }

  public abstract static class Wrap<T> extends Val {
    protected final T val;
    public Wrap(T t) { val = t; }
  }

  public static final class Bool extends Wrap<Boolean> {
    public Bool(Boolean val) { super(val); }
    public final HashMap<String, Val> getMeta() { return MMBool; }
  }

  public static final class Num extends Wrap<Double> {
    public static Num from(Double s) { return new Num(s); }
    public Num(Double val) { super(val); }
    public final HashMap<String, Val> getMeta() { return MMNum; }
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

  public abstract static class BuiltinFunc extends Val {
    public final String name;
    public BuiltinFunc(String name) { this.name = name; }
    public final HashMap<String, Val> getMeta() { return MMBuiltinFunc; }
    public abstract Val call(Val self, ArrayList<Val> args);
  }

  public static final class Blob extends Val {
    public final HashMap<String, Val> meta;
    public final HashMap<String, Val> attrs;
    public Blob(HashMap<String, Val> meta, HashMap<String, Val> attrs) {
      this.meta = meta;
      this.attrs = attrs;
    }
    public final HashMap<String, Val> getMeta() { return MMList; }
  }

  // HashMap Builder
  private static class Hmb {
    public final HashMap<String, Val> hm = new HashMap<String, Val>();
    public Hmb put(String name, Val value) {
      hm.put(name, value);
      return this;
    }
  }
}
