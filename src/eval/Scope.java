import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

public final class Scope {
  public final HashMap<String, Val> table;
  public final Scope parent;
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Val>();
  }
  public Scope() { this(GLOBAL); }
  public Val getOrNull(String name) {
    Val value = table.get(name);
    if (value == null && parent != null)
      return parent.getOrNull(name);
    return value;
  }
  public Scope put(String name, Val value) {
    table.put(name, value);
    return this;
  }
  public Scope put(BuiltinFunc bf) {
    return put(bf.name, bf);
  }
  public Scope put(HashMap<String,Val> bf) {
    return put(
        bf.get("name").as(Str.class, "FUBAR").val, new Blob(Val.MMMeta, bf));
  }

  /* package-private */ Scope put(Blob m) {
    System.out.println("XXX" + m.attrs.get("name").as(Str.class, "FUBAR").val);
    return put(m.attrs.get("name").as(Str.class, "FUBAR").val, m);
  }

  public Val eval(Ast ast) {
    // TODO: Once Evaler is ready, uncomment following line.
    // return new Evaler(this).eval(ast);
    return new Evaluator(this).visit(ast);
  }

  private static final Scope GLOBAL = new Scope(null)
      .put("nil", Nil.val)
      .put("true", Bool.tru)
      .put("false", Bool.fal)
      .put(Val.MMMeta)
      .put(Val.MMVal)
      .put(Nil.MM)
      .put(Bool.MM)
      .put(Num.MM)
      .put(Str.MM)
      .put(List.MM)
      .put(Map.MM)
      .put(Func.MM)
      .put(BuiltinIter.MM)
      .put(new BuiltinFunc("new") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return new Blob(args.get(0).as(Blob.class, "arg"));
        }
      })
      .put(new BuiltinFunc("L") {
        public Val calli(Val self, ArrayList<Val> args) {
          return List.from(args);
        }
      })
      .put(new BuiltinFunc("M") {
        public Val calli(Val self, ArrayList<Val> args) {
          if (args.size()%2 != 0)
            throw new Err(
                "'M' requires an even number of arguments, but got " +
                args.size());

          HashMap<Val, Val> attrs = new HashMap<Val, Val>();
          Iterator<Val> it = args.iterator();
          while (it.hasNext()) {
            Val key = it.next();
            Val val = it.next();
            attrs.put(key, val);
          }

          return new Map(attrs);
        }
      })
      .put(new BuiltinFunc("err") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          throw new Err(args.get(0)
              .call("str").as(Str.class, "result of method str").val);
        }
      })
      ;
  static {
    GLOBAL.put("GLOBAL", new Blob(new HashMap<String, Val>(), GLOBAL.table));
  }
}
