public class Xccl {

  // Create Obj from java types.
  public static Obj X(Boolean b) { return b ? TRUE : FALSE; }
  public static Obj X(Double d) { return new Num(d); }
  public static Obj X(Integer i) { return new Num(i); }
  public static Obj X(String s) { return new Str(s); }
  public static Obj A(Obj... a) { return new List(a); }
  public static Obj D(Obj... d) { return new Dict(d); }

  public static final Obj NIL = new Nil();
  public static final Obj TRUE = new Bool(true);
  public static final Obj FALSE = new Bool(false);
  public static final Obj ZERO = X(0);
  public static final Obj ONE = X(1);

  public static abstract class Obj {
    public static int objcnt = 0;
    public final int uid;

    protected Obj() { uid = objcnt++; }

    // operations
    public Obj id() { return X(uid); }
    public Obj is(Obj r) { return X(this.uid == r.uid); }

    public Obj truth() { throw getException(); }
    public Obj not() { return X(!toBoolean()); }

    public Obj eq(Obj r) { throw getException(); }
    public Obj ne(Obj r) { return eq(r).not(); }
    public Obj lt(Obj r) { throw getException(); }
    public Obj le(Obj r) { throw getException(); }
    public Obj gt(Obj r) { throw getException(); }
    public Obj ge(Obj r) { throw getException(); }

    public Obj add(Obj r) { throw getException(); }
    public Obj sub(Obj r) { throw getException(); }
    public Obj mul(Obj r) { throw getException(); }
    public Obj div(Obj r) { throw getException(); }
    public Obj mod(Obj r) { throw getException(); }
    public Obj pow(Obj r) { throw getException(); }
    public Obj abs() { throw getException(); }
    public Obj neg() { throw getException(); }

    public Obj iadd(Obj r) { throw getException(); }

    public Obj size() { throw getException(); }
    public Obj getitem(Obj k) { throw getException(); }
    public Obj setitem(Obj k, Obj v) { throw getException(); }

    public Obj getattr(Obj k) { throw getException(); }
    public Obj setattr(Obj k, Obj v) { throw getException(); }

    public Obj push(Obj v) { throw getException(); }
    public Obj pop() { throw getException(); }
    public Obj pop(Obj k) { throw getException(); }

    public Obj contains(Obj k) { throw getException(); }

    public Obj str() { return repr(); }
    public Obj repr() { throw getException(); }

    public Obj call(Obj... args) { throw getException(); }

    // To java types
    public int hashCode() { return 0; }
    public boolean equals(Object r) { return (r instanceof Obj) && eq((Obj) r).toBoolean(); }
    public RuntimeException getException() { return new RuntimeException(getClass().getName()); }
    public Boolean toBoolean() { return truth().toBoolean(); }
    public Double toDouble() { throw getException(); }
    public Integer toInteger() { return toDouble().intValue(); }
    public String toString() { return str().toString(); }
    public java.util.ArrayList<Obj> toArrayList() { throw getException(); }
  }

  public static class Nil extends Obj {
  }

  public static class Bool extends Obj {
    private final Boolean value;
    public Bool(Boolean b) { value = b; }

    public Boolean toBoolean() { return value; }
  }

  public static class Num extends Obj {
    private final Double value;
    public Num(Double d) { value = d; }
    public Num(Integer i) { value = i.doubleValue(); }

    public Double toDouble() { return value; }
  }

  public static class Str extends Obj {
    private final String value;
    public Str(String s) { value = s; }

    public Obj truth() { return X(value.length() > 0); }

    public Obj eq(Obj r) { return X((r instanceof Str) && (r.toString().equals(value))); }
    public Obj lt(Obj r) { throw getException(); }

    public Obj add(Obj r) { throw getException(); }
    public Obj mod(Obj r) { throw getException(); }

    public Obj iadd(Obj r) { throw getException(); }

    public Obj size() { return X(value.length()); }
    public Obj getitem(Obj k) { throw getException(); }
    public Obj setitem(Obj k, Obj v) { throw getException(); }

    public Obj push(Obj v) { throw getException(); }
    public Obj pop() { throw getException(); }

    public Obj contains(Obj k) { throw getException(); }

    public Obj str() { return this; }
    public Obj repr() { return X(value.replace("\"", "\\\"").replace("\n", "\\n")); }

    public String toString() { return value; }
  }

  public static class List extends Obj {
    private final java.util.ArrayList<Obj> value = new java.util.ArrayList<Obj>();
    public List(Obj... args) {
      for (int i = 0; i < args.length; i++)
        value.add(args[i]);
    }

    public Obj truth() { return X(value.size() > 0); }

    public Obj eq(Obj r) { throw getException(); }
    public Obj ne(Obj r) { return eq(r).not(); }
    public Obj lt(Obj r) { throw getException(); }
    public Obj le(Obj r) { throw getException(); }
    public Obj gt(Obj r) { throw getException(); }
    public Obj ge(Obj r) { throw getException(); }

    public Obj add(Obj r) { throw getException(); }
    public Obj sub(Obj r) { throw getException(); }
    public Obj mul(Obj r) { throw getException(); }
    public Obj div(Obj r) { throw getException(); }
    public Obj mod(Obj r) { throw getException(); }
    public Obj pow(Obj r) { throw getException(); }
    public Obj abs() { throw getException(); }
    public Obj neg() { throw getException(); }

    public Obj iadd(Obj r) { throw getException(); }

    public Obj size() { return X(value.size()); }
    public Obj getitem(Obj k) { return value.get(k.toInteger()); }
    public Obj setitem(Obj k, Obj v) { throw getException(); }

    public Obj getattr(Obj k) { throw getException(); }
    public Obj setattr(Obj k, Obj v) { throw getException(); }

    public Obj push(Obj v) { throw getException(); }
    public Obj pop() { throw getException(); }
    public Obj pop(Obj k) { throw getException(); }

    public Obj contains(Obj k) { throw getException(); }

    public Obj str() { return repr(); }
    public Obj repr() { throw getException(); }

    public Obj call(Obj... args) { throw getException(); }

  }

  public static class Dict extends Obj {
    private final java.util.HashMap<Obj, Obj> value = new java.util.HashMap<Obj, Obj>();
    public Dict(Obj... args) {
      if (args.length % 2 != 0)
        throw new RuntimeException("Dict constructor requires an even number of arguments");
      for (int i = 0; i < args.length; i += 2)
        value.put(args[i], args[i+1]);
    }

    public Obj truth() { return X(value.size() > 0); }

    public Obj eq(Obj r) { return X((r instanceof Dict) && ((Dict) r).value.equals(value)); }

    public Obj add(Obj r) { throw getException(); }

    public Obj iadd(Obj r) { throw getException(); }

    public Obj size() { return X(value.size()); }
    public Obj getitem(Obj k) { if (value.containsKey(k)) return value.get(k); else throw getException(); }
    public Obj setitem(Obj k, Obj v) { return value.put(k, v); }

    public Obj pop(Obj k) { throw getException(); }

    public Obj contains(Obj k) { throw getException(); }

    public Obj repr() { throw getException(); }

  }

  public static class Lambda extends Obj {
    public final Obj context;
    public final Obj node;
    public Lambda(Obj c, Obj n) { context = c; node = n; }
  }

  public static class UserObj extends Obj {
    // TODO
  }

  //-------- Runtime

  public final Obj MAIN_FILESPEC;
  public final Obj CODE_REGISTRY = D();

  protected Xccl(Obj main_filespec) {
    MAIN_FILESPEC = main_filespec;
  }

  public void run() {
    eval(getRootContext(), CODE_REGISTRY.getitem(MAIN_FILESPEC));
  }

  public Obj getRootContext() {
    return D();
  }

  public Obj eval(Obj context, Obj node) {
    Obj type = node.getitem(X("type"));
    if (type.eq(X("Module")).toBoolean()) {
      Obj last = X(0);
      Obj stmts = node.getitem(X("stmts"));
      int bound = stmts.size().toInteger();
      for (int i = 0; i < bound; i++)
        last = eval(context, stmts.getitem(X(i)));
      return last;
    }
    throw new RuntimeException("Unrecognized node: " + type.toString());
  }
  // Lexer

  public static final Obj SYMBOLS = A(
      X("("), X(")"), X("["), X("]"), X("{"), X("}"),
      X("."), X(","), X(";"), X("\\"),
      X("=")
  );

  public static final Obj KEYWORDS = A(
      X("pass"),
      X("var"),
      X("return"),
      X("while"), X("break"), X("continue"),
      X("if"), X("else"),
      X("nil"),
      X("true"), X("false")
  );
}
