import java.util.HashMap;

public final class Scope {
  public final HashMap<String, Val> table;
  public final Scope parent;
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Val>();
  }
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
  // public Scope put(BuiltinFunc bf) {
  //   return put(bf.name, bf);
  // }
  // public Scope put(Map m) {
  //   String n = asStr(m.getVal().get(toStr("__name__")), "FUBAR").getVal();
  //   return put(n, m);
  // }
}
