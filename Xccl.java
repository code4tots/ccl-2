public class Xccl {

  public static final Obj NIL = Obj.NIL;
  public static final Obj TRUE = Obj.TRUE;
  public static final Obj FALSE = Obj.FALSE;
  public static Obj X(Integer i) { return Obj.X(i); }
  public static Obj X(Double d) { return Obj.X(d); }
  public static Obj X(String s) { return Obj.X(s); }
  public static Obj A(Obj... args) { return Obj.A(args); }
  public static Obj D(Obj... args) { return Obj.D(args); }

  //-------- Runtime

  public final Obj MAIN_FILESPEC;
  public final Obj CODE_REGISTRY = D();

  protected Xccl(Obj main_filespec) {
    MAIN_FILESPEC = main_filespec;
  }

  public void run() {
    eval(getRootContext(), CODE_REGISTRY.getattr("__getitem__").call(MAIN_FILESPEC));
  }

  public Obj getRootContext() {
    return D();
  }

  public void assign(Obj context, Obj target, Obj value) {
    Obj type = target.getattr("__getitem__").call(X("type"));
    if (type.eq(X("Name"))) {
      context.getattr("__setitem__").call(target.getattr("__getitem__").call(X("name")), value);
    }
    else {
      throw new RuntimeException("You can't assign to " + type.toString());
    }
  }

  public Obj eval(Obj context, Obj node) {
    Obj type = node.getattr("__getitem__").call(X("type"));
    if (type.eq(X("Module"))) {
      Obj last = X(0);
      Obj stmts = node.getattr("__getitem__").call(X("stmts"));
      int bound = stmts.getattr("size").call().toInteger();
      for (int i = 0; i < bound; i++)
        last = eval(context, stmts.getattr("__getitem__").call(X(i)));
      return last;
    }
    else if (type.eq(X("Block"))) {
      Obj last = X(0);
      Obj stmts = node.getattr("__getitem__").call((X("stmts")));
      int bound = stmts.getattr("size").call().toInteger();
      for (int i = 0; i < bound; i++)
        last = eval(context, stmts.getattr("__getitem__").call(X(i)));
    }
    else if (type.eq(X("Declaration"))) {
      Obj value = eval(context, node.getattr("__getitem__").call(X("value")));
      assign(context, node.getattr("__getitem__").call(X("target")), value);
      return value;
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

  public static void main(String[] args) {
  }
}
