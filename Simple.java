import java.util.ArrayList;
import java.util.HashMap;

public class Simple {

public final NilVal NIL = new NilVal();
public final BoolVal TRUE = new BoolVal(true);
public final BoolVal FALSE = new BoolVal(false);

public abstract class Val {
  public final boolean equals(Object x) {
    return (x instanceof Val) && equals((Val) x);
  }
}
public final class NilVal extends Val {
  public final boolean equals(Val x) { return x instanceof NilVal; }
  public final int hashCode() { return 0; }
}
public final class BoolVal extends Val {
  public final Boolean val;
  public BoolVal(Boolean val) { this.val = val; }
  public final boolean equals(Val x) {
    return (x instanceof BoolVal) && ((BoolVal) x).val.equals(val);
  }
  public final int hashCode() { return val ? 1 : 0; }
}
public final class StrVal extends Val {
  public final String val;
  public StrVal(String val) { this.val = val; }
  public final boolean equals(Val x) {
    return (x instanceof StrVal) && ((StrVal) x).val.equals(val);
  }
  public final int hashCode() { return val.hashCode(); }
}
public final class NumVal extends Val {
  public final Double val;
  public NumVal(Double val) { this.val = val; }
  public final boolean equals(Val x) {
    return (x instanceof NumVal) && ((NumVal) x).val.equals(val);
  }
  public final int hashCode() { return val.hashCode(); }
}
public final class ListVal extends Val {
  public final ArrayList<Val> val;
  public ListVal(ArrayList<Val> val) { this.val = val; }
  public final boolean equals(Val x) {
    return (x instanceof ListVal) && ((ListVal) x).val.equals(val);
  }
  public final int hashCode() { return val.hashCode(); }
}
public final class DictVal extends Val {
  public final HashMap<Val, Val> val;
  public DictVal(HashMap<Val, Val> val) { this.val = val; }
  public final boolean equals(Val x) {
    return (x instanceof DictVal) && ((DictVal) x).val.equals(val);
  }
  public final int hashCode() { return val.hashCode(); }
}
public abstract class FuncVal extends Val {
  public final String name;
  public final int arglen;
  public FuncVal(String name, int arglen) {
    this.name = name;
    this.arglen = arglen;
  }
  public final Val call(Trace trace, ArrayList<Val> args) {
    if (args.size() != arglen)
      throw err(
          trace,
          "Expected " + Integer.toString(arglen) + " arguments " +
          "when calling function " + name + " " +
          "but found " + Integer.toString(args.size()));
    return calli(new Trace(trace, this), args);
  }
  protected abstract Val calli(Trace trace, ArrayList<Val> args);
}
public final class UserFuncVal extends FuncVal {
  public final ArrayList<String> args;
  public final Ast body;
  public final Scope scope;
  public UserFuncVal(String name, ArrayList<String> args, Ast body, Scope scope) {
    super(name, args.size());
    this.args = args;
    this.body = body;
    this.scope = scope;
  }
  protected final Val calli(Trace trace, ArrayList<Val> args) {
    Scope scope = new Scope(this.scope);
    for (int i = 0; i < args.size(); i++)
      scope.put(this.args.get(i), args.get(i));
    return body.eval(scope, trace);
  }
}

public abstract class Ast {
  public abstract Val eval(Scope scope, Trace trace);
}
public final class NameAst extends Ast {
  public final String name;
  public NameAst(String name) { this.name = name; }
  public final Val eval(Scope scope, Trace trace) {
    Val result = scope.getOrNull(name);
    if (result == null)
      throw err(trace, "No variable named " + name);
    return result;
  }
}
public final class StrAst extends Ast {
  public final StrVal val;
  public StrAst(String val) { this.val = new StrVal(val); }
  public final Val eval(Scope scope, Trace trace) {
    return val;
  }
}
public final class NumAst extends Ast {
  public final NumVal val;
  public NumAst(Double val) { this.val = new NumVal(val); }
  public final Val eval(Scope scope, Trace trace) {
    return val;
  }
}
public final class CallAst extends Ast {
  public final Ast f;
  public final ArrayList<Ast> args;
  public CallAst(Ast f, ArrayList<Ast> args) {
    this.f = f;
    this.args = args;
  }
  public final Val eval(Scope scope, Trace trace) {
    Val f = this.f.eval(scope, trace);
    if (!(f instanceof FuncVal))
      throw err(
          trace, "Tried to use " + f.getClass().getName() + " as a function");
    ArrayList<Val> args = new ArrayList<Val>();
    for (int i = 0; i < this.args.size(); i++)
      args.add(this.args.get(i).eval(scope, trace));
    return ((FuncVal) f).call(trace, args);
  }
}
public final class AssignAst extends Ast {
  public final String name;
  public final Ast val;
  public AssignAst(String name, Ast val) {
    this.name = name;
    this.val = val;
  }
  public final Val eval(Scope scope, Trace trace) {
    Val v = val.eval(scope, trace);
    scope.put(name, v);
    return v;
  }
}
public final class BlockAst extends Ast {
  public final ArrayList<Ast> stmts;
  public BlockAst(ArrayList<Ast> stmts) {
    this.stmts = stmts;
  }
  public final Val eval(Scope scope, Trace trace) {
    Val v = NIL;
    for (int i = 0; i < stmts.size(); i++)
      v = stmts.get(i).eval(scope, trace);
    return v;
  }
}

public final class Trace {
  public final Trace parent;
  public final FuncVal f;
  public Trace(Trace parent, FuncVal f) {
    this.parent = parent;
    this.f = f;
  }
}

public static final class Scope {
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
  public Scope put(FuncVal f) {
    return put(f.name, f);
  }
}

public static RuntimeException err(Trace trace, String message) {
  return new RuntimeException(message + toString(trace));
}

public static String toString(Trace trace) {
  StringBuilder sb = new StringBuilder();
  while (trace != null)
    sb.append("in function " + trace.f.name + "\n");
  return sb.toString();
}

}
