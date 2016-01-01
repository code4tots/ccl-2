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

  public Val eval(Ast ast) { return new Evaluator(this).visit(ast); }

  private static final Scope GLOBAL = new Scope(null)
      .put(Val.MMMeta)
      .put(Val.MMVal)
      .put(Nil.MM)
      .put(Bool.MM)
      .put(Num.MM)
      .put(Str.MM)
      .put(List.MM)
      .put(Map.MM)
      .put(Func.MM)
      ;
}
