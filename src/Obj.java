// Quick test: javac -Xlint *.java && java -ea Obj

import java.util.ArrayList;
import java.util.HashMap;

// TODO: Python has a sort of a 'lazy inheritance'.
// For simplicity, I use a sort of 'strict inheritance' here.
// Maybe change this in the future.
// (lazy vs. strict is with respect to the attribute binding).

public class Obj {

  // ----- static -----
  public static int OBJ_COUNT = 0;

  public static final Obj TYPE_TYPE = new Obj();
  public static final Obj OBJ_TYPE = new Obj(TYPE_TYPE);

  public static final Obj NIL_TYPE = Nil.TYPE;
  public static final Obj BOOL_TYPE = Bool.TYPE;
  public static final Obj NUM_TYPE = Num.TYPE;
  public static final Obj STR_TYPE = Str.TYPE;
  public static final Obj LIST_TYPE = List.TYPE;
  public static final Obj DICT_TYPE = Dict.TYPE;
  public static final Obj FUNCTION_TYPE = Function.TYPE;
  public static final Obj METHOD_TYPE = Method.TYPE;
  public static final Obj LAMBDA_TYPE = Lambda.TYPE;

  public static final Obj NIL = Nil.NIL;
  public static final Obj TRUE = Bool.TRUE;
  public static final Obj FALSE = Bool.FALSE;

  public static Obj X(Boolean x) { return x ? TRUE : FALSE; }
  public static Obj X(Integer x) { return Num.X(x); }
  public static Obj X(Double x) { return Num.X(x); }
  public static Obj X(String x) { return Str.X(x); }
  public static Obj A(Obj... x) { return List.A(x); }
  public static Obj D(Obj... x) { return Dict.D(x); }
  public static Obj M(Obj self, Obj callable) { return new Method(self, callable); }
  public static Obj L(Obj context, Obj node) { return new Lambda(context, node); }

  public static RuntimeException err(String message) {
    return new RuntimeException(message);
  }

  public static Obj eval(Obj context, Obj node) {
    String type = node.m("__getitem__", X("type")).toString();
    // TODO
    throw err("Unrecognized node type: " + type);
  }

  // ----- non-static ----
  public final int uid;
  public final Obj type;
  public final HashMap<String, Obj> attrs = new HashMap<String, Obj>();

  // This constructor should be called exactly once.
  private Obj() {
    uid = OBJ_COUNT++;
    type = this;
  }

  // All Obj aside from TYPE_TYPE should use this constructor.
  public Obj(Obj type) {
    uid = OBJ_COUNT++;
    this.type = type;
  }

  public boolean hasattr(String name) {
    return getattr(name) != null;
  }

  public Obj getattr(String name) {
    Obj attr = attrs.get(name);
    if (attr == null) {
      attr = type.attrs.get(name);
      if (attr == null) {
        Obj mroobj = type.attrs.get("__mro__");
        if (mroobj != null) {
          ArrayList<Obj> mro = mroobj.toArrayList();
          for (int i = 0; i < mro.size(); i++) {
            attr = mro.get(i).attrs.get(name);
            if (attr != null)
              break;
          }
        }
      }
      if (attr instanceof Callable)
        attr = M(this, attr);
    }
    return attr;
  }

  public Obj xgetattr(String name) {
    Obj attr = getattr(name);
    if (attr == null)
      throw err("No attribute with name: " + name);
    return attr;
  }

  public void setattr(String name, Obj value) {
    attrs.put(name, value);
  }

  public Obj call(Obj... args) {
    return xgetattr("__call__").call(args);
  }

  // ----- Convenience/Java interaction methods -----

  public final Obj m(String name, Obj... args) {
    return xgetattr(name).call(args);
  }

  public final int hashCode() {
    Obj x = m("__hash__");
    if (!(x instanceof Num))
      throw err("__hash__ must return an instnce of num");
    return x.toInteger();
  }

  public final boolean equals(Object other) {
    if (!(other instanceof Obj))
      return false;
    return m("__eq__", (Obj) other).toBoolean();
  }

  // Should only be overriden by Bool. Otherwise should be considered final.
  public Boolean toBoolean() {
    Obj x = m("__bool__");
    if (!(x instanceof Bool))
      throw err("__bool__ must return an instance of bool");
    return x.toBoolean();
  }

  // Should only be overriden by Num. Otherwise should be considered final.
  public Double toDouble() {
    Obj x = m("__num__");
    if (!(x instanceof Bool))
      throw err("__num__ must reutrn an instance of num");
    return x.toDouble();
  }

  public final Integer toInteger() {
    return toDouble().intValue();
  }

  // Should only be overriden by Str. Otherwise should be considered final.
  public String toString() {
    Obj x = m("__str__");
    if (!(x instanceof Str))
      throw err("__str__ must return an instance of str");
    return x.toString();
  }

  // Should only be overriden by List. Otherwise should be considered final.
  public ArrayList<Obj> toArrayList() {
    throw err("Convertion ot ArrayList not supported");
  }

  public static void main(String[] args) {
    Nil.main(args);
    Bool.main(args);
    Num.main(args);
    Str.main(args);
    List.main(args);
    Dict.main(args);
    Function.main(args);
    Method.main(args);
  }
}

