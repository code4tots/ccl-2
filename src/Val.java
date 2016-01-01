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
      .put("__call__", new BuiltinFunc("__call__") {
        public Val calli(Val self, ArrayList<Val> args) {
          return self.as(BuiltinFunc.class, "self").call(self, args);
        }
      })
      .hm;

  public static final HashMap<String, Val> MMUserFunc = new Hmb()
      .put("name", Str.from("UserFunc"))
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

  public static final class UserFunc extends Func {
    public final Token token;
    public final ArrayList<String> args;
    public final String vararg;
    public final Ast body;
    public final Scope scope;
    public UserFunc(
        Token token, ArrayList<String> args, String vararg,
        Ast body, Scope scope) {
      this.token = token;
      this.args = args;
      this.vararg = vararg;
      this.body = body;
      this.scope = scope;
    }
    public final HashMap<String, Val> getMeta() { return MMUserFunc; }
    public final String getTraceMessage() {
      return "\nin user function defined in " + token.getLocationString();
    }
    public final Val call(Val self, ArrayList<Val> args) {
      Scope scope = new Scope(this.scope);
      if (vararg == null && this.args.size() != args.size())
        throw new Err(
            "Expected " + this.args.size() + " arguments but found " +
            args.size());
      if (vararg != null && this.args.size() > args.size())
        throw new Err(
            "Expected at least " + this.args.size() +
            " arguments but found only " + args.size());

      for (int i = 0; i < this.args.size(); i++)
        scope.put(this.args.get(i), args.get(i));

      if (vararg != null) {
        ArrayList<Val> va = new ArrayList<Val>();
        for (int i = this.args.size(); i < args.size(); i++)
          va.add(args.get(i));
        scope.put(vararg, Val.List.from(va));
      }

      return scope.eval(body);
    }
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
  private static class Hmb {
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
