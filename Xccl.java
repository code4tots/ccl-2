public class Xccl {

  public static final Obj NIL = Obj.NIL;
  public static final Obj TRUE = Obj.TRUE;
  public static final Obj FALSE = Obj.FALSE;
  public static Obj X(Integer i) { return Obj.X(i); }
  public static Obj X(Double d) { return Obj.X(d); }
  public static Obj X(String s) { return Obj.X(s); }
  public static Obj A(Obj... args) { return Obj.A(args); }
  public static Obj D(Obj... args) { return Obj.D(args); }

  public static final Obj LAMBDA_TYPE = new Obj.Type(Obj.CALLABLE_TYPE);

  public static class Lambda extends Obj.Callable {
    public Obj context, node;
    public Lambda(Obj context, Obj node) {
      super(LAMBDA_TYPE);
      this.context = context;
      this.node = node;
    }
    public Obj call(Obj... args) {
      Obj ctx = D(X("__parent__"), context);
      // TODO: Fill ctx with argument values.
      return eval(ctx, node.getattr("__getitem__").call(X("body")));
    }
  }

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

  public static Obj findContainingContext(Obj context, Obj name) {
    if (context.getattr("__contains__").call(name)) {
      return context;
    }
    else if (context.getattr("__contains__").call(X("__parent__"))) {
      return findContainingContext(context, name);
    }
    return null;
  }

  public static void assign(Obj context, Obj target, Obj value) {
    Obj type = target.getattr("__getitem__").call(X("type"));
    if (type.eq(X("Name"))) {
      Obj name = target.getattr("__getitem__").call(X("name"));
      context.getattr("__setitem__").call(name, value);
    }
    else {
      throw new RuntimeException("You can't assign to " + type.toString());
    }
  }

  public static Obj eval(Obj context, Obj node) {
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
      return last;
    }
    else if (type.eq(X("Declaration"))) {
      Obj value = eval(context, node.getattr("__getitem__").call(X("value")));
      assign(context, node.getattr("__getitem__").call(X("target")), value);
      return value;
    }
    else if (type.eq(X("Expression"))) {
      return eval(context, node.getattr("__getitem__").call(X("expr")));
    }
    else if (type.eq(X("Name"))) {
      return 
    }
    else if (type.eq(X("Lambda"))) {
      return new Lambda(context, node);
    }
    else if (type.eq(X("__call__"))) {
      Obj f = eval(context, node.getattr("__getitem__").call(X("f")));
      Obj argexprs = node.getattr("__getitem__").call(X("args"));
      Obj[] args = new Obj[argexprs.getattr("size").call().toInteger()];
      for (int i = 0; i < args.length; i++)
        args[i] = eval(context, argexprs.getattr("__getitem__").call(X(i)));
      return f.call(args);
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
