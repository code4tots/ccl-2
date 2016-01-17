package com.ccl;

import java.util.HashMap;
import java.util.ArrayList;

public class Runtime {

public static final Nil nil = Nil.nil;
public static final Blob META_META = Blob.META_META;
public static final Blob META_NIL = Blob.from(META_META);
public static final Blob META_STRING = Blob.from(META_META);
public static final Blob META_LIST = Blob.from(META_META);
public static final Blob META_SCOPE = Blob.from(META_META);
public static final Blob META_FUNCTION = Blob.from(META_META);

public static final Scope GLOBAL = new Scope(null)
  .put(new BuiltinFunction("print") {
    public Value call(Value self, List args) {
      System.out.println(args.get(0));
      return nil;
    }
  });

public abstract static class Value {
  public abstract Blob getMeta();
  public abstract String getTypeName();
  public final Value call(Value self, Value[] args, Value vararg) {
    Value result = call(self, List.from(args, vararg));
    if (result == null)
      throw new Err("Function returned null!");
    return result;
  }
  public Value call(Value self, List args) {
    throw new Err("Calling not supported for " + getTypeName());
  }
  public <T extends Value> T as(Class<T> cls) { return cls.cast(this); }
}

public static final class Nil extends Value {
  public static Nil nil = new Nil();
  private Nil() {}
  @Override public Blob getMeta() { return META_NIL; }
  @Override public String getTypeName() { return "Nil"; }
}

public static final class Str extends Value {
  public static Str from(String s) { return new Str(s); }

  private final String value;
  public Str(String s) { value = s; }
  @Override public Blob getMeta() { return META_STRING; }
  @Override public String getTypeName() { return "String"; }
  public String toString() { return value; }
}

public static final class List extends Value {
  public static List from(Value... args) {
    ArrayList<Value> list = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      list.add(args[i]);
    return new List(list);
  }
  public static List from(Value[] args, Value vararg) {
    ArrayList<Value> list = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      list.add(args[i]);
    if (vararg != null)
      list.addAll(vararg.as(List.class).value);
    return new List(list);
  }
  private final ArrayList<Value> value;
  private List(ArrayList<Value> value) { this.value = value; }
  @Override public Blob getMeta() { return META_LIST; }
  @Override public String getTypeName() { return "List"; }
  public Value get(int i) { return value.get(i); }
}

public static final class Scope extends Value {
  private final Scope parent;
  private final HashMap<String, Value> attrs =
    new HashMap<String, Value>();

  public Scope(Scope parent) { this.parent = parent; }
  @Override public Blob getMeta() { return META_SCOPE; }
  @Override public String getTypeName() { return "Scope"; }
  public Value getvar(String name) {
    Value v = attrs.get(name);
    if (v == null) {
      if (parent == null)
        throw new Err("No variable named " + name);
      return parent.getvar(name);
    }
    return v;
  }
  public Value putvar(String name, Value var) {
    attrs.put(name, var);
    return var;
  }
  public Scope put(String name, Value var) {
    attrs.put(name, var);
    return this;
  }
  public Scope put(BuiltinFunction f) {
    return put(f.name, f);
  }
}

public static final class Blob extends Value {
  public static final Blob META_META = new Blob();

  public static Blob from(Blob meta) { return new Blob(meta); }

  private final Blob meta;
  private final HashMap<String, Value> attrs =
    new HashMap<String, Value>();

  private Blob() { meta = this; }
  private Blob(Blob meta) { this.meta = meta; }

  @Override public Blob getMeta() { return meta; }
  @Override public String getTypeName() {
    Value namevalue = attrs.get("__name__");
    if (namevalue == null)
      return "Blob";
    else
      return namevalue.toString();
  }
}

public abstract static class Function extends Value {
  @Override public Blob getMeta() { return META_FUNCTION; }
}

public abstract static class BuiltinFunction extends Function {
  public final String name;
  public BuiltinFunction(String name) { this.name = name; }
  public abstract Value call(Value self, List args);
  @Override public String getTypeName() { return "Function " + name; }
}

public static final class Err extends RuntimeException {
  public Err(String msg) { super(msg); }
}

}
