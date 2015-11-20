import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;

public class Easy {

  static public void main(String[] args) {}

// scope

static public final class Scope {
  private final Scope parent;
  private final HashMap<String, Value> table;
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Value>();
  }
  public Value get(String name) {
    Value value = table.get(name);
    if (value == null) {
      if (parent == null)
        throw new RuntimeException(name);
      else
        return parent.get(name);
    }
    return value;
  }
  public void put(String name, Value value) {
    table.put(name, value);
  }
}


// value

static public abstract class Value extends Easy {
  public abstract boolean isTruthy();
  public Value getAttribute(String name) {
    throw new RuntimeException(); // TODO
  }
  public void setAttribute(String name, Value value) {
    throw new RuntimeException(); // TODO
  }
  public Value callMethod(String name, ArrayList<Value> args) {
    throw new RuntimeException(); // TODO
  }
  public Value callMethod(String name, Value... args) {
    ArrayList<Value> arglist = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      arglist.add(args[i]);
    return callMethod(name, arglist);
  }
}

static public class NilValue extends Value {
  private NilValue() {}
  public boolean isTruthy() { return false; }
  public static final NilValue nil = new NilValue();
}

public static final NilValue nil = NilValue.nil;

static public class NumberValue extends Value {
  public final Double value;
  public NumberValue(Double value) {
    this.value = value;
  }
  public boolean isTruthy() {
    return !value.equals(0.0);
  }
}
static public class StringValue extends Value {
  public final String value;
  public StringValue(String value) {
    this.value = value;
  }
  public boolean isTruthy() {
    return value.length() != 0;
  }
}

static public class ListValue extends Value {
  public final ArrayList<Value> value;
  public ListValue(ArrayList<Value> value) {
    this.value = value;
  }
  public boolean isTruthy() {
    return value.size() != 0;
  }
}

// ast
static public abstract class Ast extends Easy {
  public abstract Value eval(Scope scope);
}

static public class NumberAst extends Ast {
  public final NumberValue value;

  public NumberAst(NumberValue value) {
    this.value = value;
  }

  public Value eval(Scope scope) {
    return value;
  }
}

static public class StringAst extends Ast {
  public final StringValue value;

  public StringAst(StringValue value) {
    this.value = value;
  }

  public Value eval(Scope scope) {
    return value;
  }
}

static public class ListAst extends Ast {
  public final ArrayList<Ast> asts;

  public ListAst(ArrayList<Ast> asts) {
    this.asts = asts;
  }

  public Value eval(Scope scope) {
    ArrayList<Value> values = new ArrayList<Value>();
    for (int i = 0; i < asts.size(); i++)
      values.add(asts.get(i).eval(scope));
    return new ListValue(values);
  }
}

static public class NameAst extends Ast {
  public final String name;

  public NameAst(String name) {
    this.name = name;
  }

  public Value eval(Scope scope) {
    return scope.get(name);
  }
}

static public class AssignAst extends Ast {
  public final String name;
  public final Ast valueAst;
  public AssignAst(String name, Ast valueAst) {
    this.name = name;
    this.valueAst = valueAst;
  }
  public Value eval(Scope scope) {
    Value value = valueAst.eval(scope);
    scope.put(name, value);
    return value;
  }
}

static public class GetAttributeAst extends Ast {
  public final Ast ownerAst;
  public final String attributeName;
  public GetAttributeAst(Ast ownerAst, String attributeName) {
    this.ownerAst = ownerAst;
    this.attributeName = attributeName;
  }
  public Value eval(Scope scope) {
    return ownerAst.eval(scope).getAttribute(attributeName);
  }
}

static public class SetAttributeAst extends Ast {
  public final Ast ownerAst, valueAst;
  public final String attributeName;
  public SetAttributeAst(Ast ownerAst, String attributeName, Ast valueAst) {
    this.ownerAst = ownerAst;
    this.attributeName = attributeName;
    this.valueAst = valueAst;
  }
  public Value eval(Scope scope) {
    Value owner = ownerAst.eval(scope), value = valueAst.eval(scope);
    owner.setAttribute(attributeName, value);
    return value;
  }
}

static public class CallMethodAst extends Ast {
  public final Ast ownerAst;
  public final String methodName;
  public final ArrayList<Ast> argumentAsts;
  public CallMethodAst(Ast ownerAst, String methodName, ArrayList<Ast> argumentAsts) {
    this.ownerAst = ownerAst;
    this.methodName = methodName;
    this.argumentAsts = argumentAsts;
  }
  public Value eval(Scope scope) {
    Value owner = ownerAst.eval(scope);
    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < argumentAsts.size(); i++)
      args.add(argumentAsts.get(i).eval(scope));
    return owner.callMethod(methodName, args);
  }
}

static public class IfAst extends Ast {
  public final Ast condition, body, otherwise;
  public IfAst(Ast condition, Ast body, Ast otherwise) {
    this.condition = condition;
    this.body = body;
    this.otherwise = otherwise;
  }

  public Value eval(Scope scope) {
    return condition.eval(scope).isTruthy() ? body.eval(scope) : otherwise.eval(scope);
  }
}

static public class WhileAst extends Ast {
  public final Ast condition, body;
  public WhileAst(Ast condition, Ast body) {
    this.condition = condition;
    this.body = body;
  }

  public Value eval(Scope scope) {
    Value value = nil;
    while (condition.eval(scope).isTruthy())
      value = body.eval(scope);
    return value;
  }
}

}
