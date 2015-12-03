import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

public class Sanity {

/// Effectively the core library.

public static final TypeValue typeValue = new TypeValue("Value");
public static final TypeValue typeType = new TypeValue("Type", typeValue);
public static final TypeValue typeNil = new TypeValue("Nil", typeValue);
public static final TypeValue typeBool = new TypeValue("Bool", typeValue);
public static final TypeValue typeNumber = new TypeValue("Number", typeValue);
public static final TypeValue typeString = new TypeValue("String", typeValue);
public static final TypeValue typeList = new TypeValue("List", typeValue);
public static final TypeValue typeMap = new TypeValue("Map", typeValue);
public static final TypeValue typeFunction = new TypeValue("Function", typeValue)
    .put(new Method("__call__") {
      public final void calli(Context c, Value owner, ArrayList<Value> args) {
        ((FunctionValue) owner).call(c, args);
      }
    });

/// Language core dispatch features (i.e. Value and Method)

public static final class Context {

  // Verify the snaity of Context
  public final void check() {
  }
}

public abstract static class Method {
  public final String name;
  public Method(String name) { this.name = name; }
  public abstract void calli(Context c, Value owner, ArrayList<Value> args);
  public final void call(Context c, Value owner, ArrayList<Value> args) {
    calli(c, owner, args);
    c.check();
  }
}

public abstract static class Value {
  public abstract TypeValue getType();
  public final void call(Context c, String name, ArrayList<Value> args) {
    Method method = getType().getMethodOrNull(name);

    if (method == null)
      err(c, "No such method " + name);

    method.call(c, this, args);
  }

  // TODO: Figure out what to do about these java bridge methods.
  // The problem is that I can't pass a 'Context' argument so when there is a
  // problem I can't get a full stack trace.
  public final boolean equals(Object x) { throw new RuntimeException("FUBAR"); }
  public final int hashCode() { throw new RuntimeException("FUBAR"); }
  public final String toString() { throw new RuntimeException("FUBAR"); }
}

public static final class TypeValue extends Value {
  public final String name;
  public final ArrayList<TypeValue> bases = new ArrayList<TypeValue>();
  public final ArrayList<TypeValue> mro = new ArrayList<TypeValue>();
  public final HashMap<String, Method> methods = new HashMap<String, Method>();
  public final TypeValue getType() { return typeType; }
  public TypeValue(String n, ArrayList<Value> bs) {
    if (n == null)
      throw new RuntimeException("FUBAR");

    name = n;
    for (int i = bs.size()-1; i >= 0; i--) {
      TypeValue base = (TypeValue) bs.get(i);
      bases.add(base);
      for (int j = base.mro.size()-1; j >=0 ; j--) {
        TypeValue ancestor = base.mro.get(j);
        boolean missing = true;
        for (int k = 0; k < mro.size(); k++) {
          if (ancestor == mro.get(k)) {
            missing = false;
            break;
          }
        }
        if (missing)
          mro.add(ancestor);
      }
    }
    mro.add(this);
    Collections.reverse(mro);
  }
  public TypeValue(String n, Value... args) {
    this(n, toArrayList(args));
  }
  public TypeValue put(Method method) {
    methods.put(method.name, method);
    return this;
  }
  public final Method getMethodOrNull(String name) {
    for (int i = 0; i < mro.size(); i++) {
      TypeValue type = mro.get(i);
      Method method = type.methods.get(name);
      if (method != null)
        return method;
    }
    return null;
  }
}

public abstract static class FunctionValue extends Value {
  public final String name;
  public FunctionValue(String name) { this.name = name; }
  public abstract void calli(Context c, ArrayList<Value> args);
  public final void call(Context c, ArrayList<Value> args) {
    calli(c, args);
    c.check();
  }
}

/// Ast

public abstract static class Ast {
  public abstract void evali(Context c);
  public final void eval(Context c) {
    evali(c);
    c.check();
  }
}

/// utils
public static ArrayList<Value> toArrayList(Value... args) {
  ArrayList<Value> al = new ArrayList<Value>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}

public static void err(Context c, String message) {
  throw new RuntimeException(message);
}

}
