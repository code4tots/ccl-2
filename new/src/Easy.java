import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

public abstract class Easy {

public static final NilValue nil = new NilValue();
public static final BoolValue trueValue = new BoolValue(true);
public static final BoolValue falseValue = new BoolValue(false);

public static final ClassValue classObject = new ClassValue()
    .put("__repr__", new Method() {
      public void call(Context c, Value owner, ArrayList<Value> args) {
        c.val = new StringValue("<object>");
      }
    })
    .put("__str__", new Method() {
      public void call(Context c, Value owner, ArrayList<Value> args) {
        owner.call(c, "__repr__", args);
      }
    });
public static final ClassValue classClass = new ClassValue(classObject);
public static final ClassValue classTrace = new ClassValue(classObject);
public static final ClassValue classScope = new ClassValue(classObject);
public static final ClassValue classNil = new ClassValue(classObject);
public static final ClassValue classBool = new ClassValue(classObject);
public static final ClassValue classString = new ClassValue(classObject)
    .put("__str__", new Method() {
      public void call(Context c, Value owner, ArrayList<Value> args) {
        c.val = owner;
      }
    })
    .put("__repr__", new Method() {
      public void call(Context c, Value owner, ArrayList<Value> args) {
        // TODO
        c.val = new StringValue("\"" + ((StringValue) owner).val + "\"");
      }
    });

public static final class Context {
  public Value val = null;
  public boolean exc = false;
  public TraceValue tr = null;
  public ScopeValue sc;
  public Context(ScopeValue sc) { this.sc = sc; }
}

public static final class Exc extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final Value val;
  public Exc(Value val) { this.val = val; }
}

public abstract static class Method {
  public abstract void call(Context c, Value owner, ArrayList<Value> args);
}

// If I use generic method, I get warning about heap pollution.
public static ArrayList<ClassValue> toArrayList(ClassValue... args) {
  ArrayList<ClassValue> al = new ArrayList<ClassValue>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}
public static ArrayList<Value> toArrayList(Value... args) {
  ArrayList<Value> al = new ArrayList<Value>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}

public abstract static class Value {
  public abstract ClassValue getType();

  public final void call(Context c, String name, Value... args) {
    call(c, name, toArrayList(args));
  }

  public final void call(Context c, String name, ArrayList<Value> args) {
    Method method = getType().getMethod(name);

    if (method == null) {
      c.exc = true; // TODO: c.val
      return;
    }

    method.call(c, this, args);
  }

  public final Value callOrThrow(String name, ArrayList<Value> args) {
    Context c = new Context(null);
    call(c, name, args);
    if (c.exc)
      throw new Exc(c.val);
    return c.val;
  }

  public final Value callOrThrow(String name, Value... args) {
    return callOrThrow(name, toArrayList(args));
  }

  // Java bridge methods
  public final boolean toBool(Context c) {
    call(c, "__bool__");
    if (!(c.val instanceof BoolValue)) {
      c.exc = true; // TODO: c.val
    }
    if (c.exc)
      return false;
    return ((BoolValue) c.val).val;
  }
  public final String toString() {
    return ((StringValue) callOrThrow("__str__")).val;
  }
}

public static final class ClassValue extends Value {
  public final HashMap<String, Method> methods;
  public final ArrayList<ClassValue> bases;
  public final ArrayList<ClassValue> mro;

  public ClassValue(ClassValue... bases) {
    this(toArrayList(bases));
  }
  public ClassValue(ArrayList<ClassValue> bases) {
    methods = new HashMap<String, Method>();
    this.bases = bases;

    ArrayList<ClassValue> mro = new ArrayList<ClassValue>();
    mro.add(this);
    for (int i = bases.size()-1; i >= 0; i--)
      for (int j = bases.get(i).mro.size()-1; j >= 0; j--)
        if (!mro.contains(bases.get(i).mro.get(j)))
          mro.add(bases.get(i).mro.get(j));
    this.mro = mro;

  }

  public final ClassValue getType() { return classClass; }

  public final Method getMethod(String name) {
    for (int i = 0; i < mro.size(); i++) {
      Method method = mro.get(i).methods.get(name);
      if (method != null)
        return method;
    }
    return null;
  }

  public final ClassValue put(String name, Method method) {
    methods.put(name, method);
    return this;
  }
}

public static final class TraceValue extends Value {
  public final ClassValue getType() { return classTrace; }
}

public static final class ScopeValue extends Value {
  public final ClassValue getType() { return classScope; }
}

public static final class NilValue extends Value {
  public final ClassValue getType() { return classNil; }
}

public static final class BoolValue extends Value {
  public final boolean val;
  public BoolValue(boolean val) { this.val = val; }
  public final ClassValue getType() { return classBool; }
}

public static final class StringValue extends Value {
  public final String val;
  public StringValue(String val) { this.val = val; }
  public final ClassValue getType() { return classString; }
}

public static final class UserValue extends Value {
  public final ClassValue type;
  public final HashMap<String, Value> attrs = new HashMap<String, Value>();
  public UserValue(ClassValue type) { this.type = type; }
  public final ClassValue getType() { return type; }
  public final UserValue put(String name, Value value) {
    attrs.put(name, value);
    return this;
  }
}

}
