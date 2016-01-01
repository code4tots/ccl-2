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

  /* package-private */ Scope put(Blob m) {
    return put(m.attrs.get("name").as(Str.class, "FUBAR").val, m);
  }

  public Val eval(Ast ast) { return new Evaluator(this).visit(ast); }

  private static final Scope GLOBAL = new Scope(null);
}
