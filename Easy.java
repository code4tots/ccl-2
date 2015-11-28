import java.util.HashMap;
import java.util.ArrayList;

public abstract class Easy {

public static final NilValue nil = new NilValue();
public static final BoolValue trueValue = new BoolValue(true);
public static final BoolValue falseValue = new BoolValue(false);

public static final ClassValue classObject =
    new ClassValue("Object")
        .put(new BuiltinMethodValue("__new__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(0, args);
            Value value = new UserValue((ClassValue) owner);
            value.get("__init__").call();
            return value;
          }
        })
        .put(new BuiltinMethodValue("__init__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(0, args);
            return nil;
          }
        });
public static final ClassValue classClass =
    new ClassValue("Class", classObject);
public static final ClassValue classNil = new ClassValue("Nil", classObject);
public static final ClassValue classBool =
    new ClassValue("Bool", classObject);
public static final ClassValue classNumber =
    new ClassValue("Number", classObject)
        .put(new BuiltinMethodValue("__add__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new NumberValue(
                owner.getNumberValue() + args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__sub__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new NumberValue(
                owner.getNumberValue() - args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__mul__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new NumberValue(
                owner.getNumberValue() * args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__div__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new NumberValue(
                owner.getNumberValue() / args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__mod__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new NumberValue(
                owner.getNumberValue() % args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("floor") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(0, args);
            return new NumberValue(Math.floor(owner.getNumberValue()));
          }
        })
        .put(new BuiltinMethodValue("frac") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(0, args);
            double value = owner.getNumberValue();
            return new NumberValue(value - Math.floor(value));
          }
        });
public static final ClassValue classString =
    new ClassValue("String", classObject)
        .put(new BuiltinMethodValue("__add__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new StringValue(
                owner.getStringValue() + args.get(0).getStringValue());
          }
        })
        .put(new BuiltinMethodValue("__mod__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            ArrayList<Value> items = args.get(0).getListValue();
            String[] aa = new String[items.size()];
            for (int i = 0; i < items.size(); i++)
              aa[i] = items.get(i).repr();
            String format = owner.getStringValue();
            return new StringValue(String.format(format, (Object[]) aa));
          }
        });
public static final ClassValue classList =
    new ClassValue("List", classObject)
        .put(new BuiltinFunctionValue("__new__") {
          public Value call(ArrayList<Value> args) {
            return new ListValue(args);
          }
        });
public static final ClassValue classMap = new ClassValue("Map", classObject);
public static final ClassValue classFunction =
    new ClassValue("Function", classObject);

public static Scope BUILTIN_SCOPE = new Scope(null)
    .put("nil", nil)
    .put("true", trueValue)
    .put("false", falseValue)
    .put(classClass)
    .put(classNil)
    .put(classBool)
    .put(classNumber)
    .put(classString)
    .put(classList)
    .put(classMap)
    .put(classFunction)
    .put(new BuiltinFunctionValue("print") {
      public Value call(ArrayList<Value> args) {
        expectArglen(1, args);
        System.out.println(args.get(0));
        return args.get(0);
      }
    });

public static void expectArglen(int len, ArrayList<Value> args) {
  if (len != args.size())
    throw new RuntimeException(
        Integer.toString(len) + " " +
        Integer.toString(args.size()));
}

public static void run(Ast ast) {
  ast.eval(new Context(new Scope(BUILTIN_SCOPE)));
}

public static abstract class Value {
  public abstract ClassValue getType();
  public Value call(ArrayList<Value> args)  {
    throw new RuntimeException(getClass().getName() + " is not callable");
  }
  public Value call(Value... args) {
    ArrayList<Value> al = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return call(al);
  }
  public void call(Context c, ArrayList<Value> args) {
    c.value = call(args);
  }
  public void call(Context c, Value... args) {
    ArrayList<Value> al = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    call(c, al);
  }
  public final Value get(String name) {
    Value value = getOrNull(name);
    if (value == null)
      throw new RuntimeException(name);
    return value;
  }
  public Value getOrNull(String name) {
    Value value = getType().getForInstance(name);
    return value == null ? null : value.bind(this);
  }
  public Value put(String name, Value value) {
    throw new RuntimeException(getClass().getName() + "." + name);
  }
  public abstract boolean isTruthy();
  public Value bind(Value owner) {
    return this;
  }
  public double getNumberValue() {
    throw new RuntimeException(getClass().getName());
  }
  public String getStringValue() {
    throw new RuntimeException(getClass().getName());
  }
  public ArrayList<Value> getListValue()  {
    throw new RuntimeException(getClass().getName());
  }
  public abstract int hashCode();
  public abstract String repr();
  public String toString() {
    return repr();
  }
}

public static final class UserValue extends Value {
  public final ClassValue cls;
  public final HashMap<String, Value> attrs;
  public UserValue(ClassValue cls) {
    this.cls = cls;
    attrs = new HashMap<String, Value>();
  }
  public ClassValue getType() {
    return cls;
  }
  public Value getOrNull(String name) {
    Value value = super.getOrNull(name);
    return value == null ? attrs.get(name) : value;
  }
  public Value put(String name, Value value) {
    attrs.put(name, value);
    return this;
  }
  public boolean isTruthy() {
    return true; // TODO
  }
  public String repr() {
    return "<UserValue>"; // TODO
  }
  public int hashCode() {
    return 0; // TODO
  }
}

public static final class NilValue extends Value {
  public ClassValue getType() {
    return classNil;
  }
  public boolean isTruthy() {
    return false;
  }
  public String repr() {
    return "nil";
  }
  public int hashCode() {
    return 0;
  }
}

public static final class BoolValue extends Value {
  public final boolean value;
  public BoolValue(boolean value) {
    this.value = value;
  }
  public ClassValue getType() {
    return classBool;
  }
  public boolean isTruthy() {
    return value;
  }
  public String repr() {
    return value ? "true" : "false";
  }
  public int hashCode() {
    return new Boolean(value).hashCode();
  }
}

public static final class StringValue extends Value {
  public final String value;
  public StringValue(String value) {
    this.value = value;
  }
  public ClassValue getType() {
    return classString;
  }
  public boolean isTruthy() {
    return value.length() != 0;
  }
  public String repr() {
    StringBuilder sb = new StringBuilder();
    sb.append("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch(c) {
      case '\n': sb.append("\\n"); break;
      case '"': sb.append("\\\""); break;
      default: sb.append(c);
      }
    }
    sb.append("\"");
    return sb.toString();
  }
  public String toString() {
    return value;
  }
  public String getStringValue() {
    return value;
  }
  public int hashCode() {
    return value.hashCode();
  }
}

public static final class NumberValue extends Value {
  public final double value;
  public NumberValue(double value) {
    this.value = value;
  }
  public ClassValue getType() {
    return classNumber;
  }
  public boolean isTruthy() {
    return value != 0.0;
  }
  public double getNumberValue() {
    return value;
  }
  public String repr() {
    if (value == Math.floor(value))
      return Integer.toString((int) value);
    return Double.toString(value);
  }
  public int hashCode() {
    return new Double(value).hashCode();
  }
}

public static final class ListValue extends Value {
  public final ArrayList<Value> value;
  public ListValue(ArrayList<Value> value) {
    this.value = value;
  }
  public ClassValue getType() {
    return classList;
  }
  public boolean isTruthy() {
    return value.size() != 0;
  }
  public int hashCode() {
    return value.hashCode();
  }
  public String repr() {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < value.size(); i++) {
      if (i != 0)
        sb.append(", ");
      sb.append(value.get(i).repr());
    }
    sb.append("]");
    return sb.toString();
  }
  public ArrayList<Value> getListValue()  {
    return value;
  }
}

public static abstract class NamedValue extends Value {
  public final String name;
  public NamedValue(String name) {
    this.name = name;
  }
  public final boolean isTruthy() {
    return true;
  }
  public final int hashCode() {
    return System.identityHashCode(this);
  }
  public String repr() {
    return "<" + getType().name + " '" + name + "'>";
  }
}

public static abstract class FunctionValue extends NamedValue {
  public FunctionValue(String name) {
    super(name);
  }
  public ClassValue getType() {
    return classFunction;
  }
}

public static abstract class BuiltinMethodValue extends FunctionValue {
  public BuiltinMethodValue(String name) {
    super(name);
  }
  public final Value call(ArrayList<Value> args) {
    throw new RuntimeException(
        "Can't call a builtin method without binding it first");
  }
  public abstract Value call(Value owner, ArrayList<Value> args);
  public Value bind(final Value owner) {
    return new BuiltinFunctionValue(name) {
      public Value call(ArrayList<Value> args) {
        return call(owner, args);
      }
    };
  }
}

public static abstract class BuiltinFunctionValue extends FunctionValue {
  public BuiltinFunctionValue(String name) {
    super(name);
  }
  public abstract Value call(ArrayList<Value> args);
}

public static final class UserMethodValue extends FunctionValue {
  public final Value owner;
  public final Scope scope;
  public final String[] args;
  public final String vararg;
  public final Ast body;
  public UserMethodValue(
      Value owner, Scope scope,
      String name, String[] args, String vararg, Ast body) {
    super(name);
    this.owner = owner;
    this.scope = scope;
    this.args = args;
    this.vararg = vararg;
    this.body = body;
  }
  public Value call(ArrayList<Value> args) {
    Scope scope = new Scope(this.scope);
    scope.put("self", owner);
    for (int i = 0; i < this.args.length; i++) {
      String name = this.args[i];
      Value value = args.get(i);
      scope.put(name, value);
    }
    if (vararg != null) {
      ArrayList<Value> va = new ArrayList<Value>();
      for (int i = this.args.length; i < args.size(); i++)
        va.add(args.get(i));
      scope.put(vararg, new ListValue(va));
    }

    Context c = new Context(scope);
    body.eval(c);
    if (c.exc && !c.ret)
      throw new RuntimeException(c.value == null ? "--null--" : c.value.toString());

    return c.value;
  }
}

public static final class UserFunctionValue extends FunctionValue {
  public final Scope scope;
  public final String[] args;
  public final String vararg;
  public final Ast body;
  public UserFunctionValue(
      Scope scope,
      String name, String[] args, String vararg, Ast body) {
    super(name);
    this.scope = scope;
    this.args = args;
    this.vararg = vararg;
    this.body = body;
  }
  public Value call(ArrayList<Value> args) {
    Scope scope = new Scope(this.scope);
    for (int i = 0; i < this.args.length; i++) {
      String name = this.args[i];
      Value value = args.get(i);
      scope.put(name, value);
    }
    if (vararg != null) {
      ArrayList<Value> va = new ArrayList<Value>();
      for (int i = this.args.length; i < args.size(); i++)
        va.add(args.get(i));
      scope.put(vararg, new ListValue(va));
    }
    Context c = new Context(scope);
    body.eval(c);
    if (c.exc && !c.ret)
      throw new RuntimeException(c.value == null ? "--null--" : c.value.toString());

    return c.value;
  }
  public Value bind(Value owner) {
    return new UserMethodValue(owner, scope, name, args, vararg, body);
  }
}

public static ArrayList<Value> valueArrayToArrayList(Value... bases) {
  ArrayList<Value> values = new ArrayList<Value>();
  for (int i = 0; i < bases.length; i++)
    values.add(bases[i]);
  return values;
}

public static final class ClassValue extends NamedValue {
  public final ArrayList<ClassValue> bases;
  public final HashMap<String, Value> attrs;
  public ClassValue(
      String name, ArrayList<Value> bases, HashMap<String, Value> attrs) {
    super(name);
    this.bases = new ArrayList<ClassValue>();
    for (int i = 0; i < bases.size(); i++)
      this.bases.add((ClassValue) bases.get(i));
    this.attrs = attrs;
  }
  public ClassValue(String name, ArrayList<Value> bases) {
    this(name, bases, new HashMap<String, Value>());
  }
  public ClassValue(String name) {
    this(name, new ArrayList<Value>());
  }
  public ClassValue(String name, Value... bases) {
    this(name, valueArrayToArrayList(bases));
  }
  public Value call(ArrayList<Value> args) {
    FunctionValue f = (FunctionValue) getForInstance("__new__");
    if (f == null) {
      throw new RuntimeException(name);
    }
    return f.bind(this).call(args);
  }
  public ClassValue getType() {
    return classClass;
  }
  public Value getForInstance(String name) {
    // TODO: C3 MRO
    Value value = attrs.get(name);
    if (value == null) {
      for (int i = 0; i < bases.size() && value == null; i++) {
        value = bases.get(i).getForInstance(name);
      }
    }
    return value;
  }
  public ClassValue put(String name, Value value) {
    attrs.put(name, value);
    return this;
  }
  public ClassValue put(FunctionValue f) {
    put(f.name, f);
    return this;
  }
}

public static final class Scope {
  public final HashMap<String, Value> table;
  public final Scope parent;
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Value>();
  }
  public Value getOrNull(String name) {
    Value value = table.get(name);
    if (value == null && parent != null)
      return parent.getOrNull(name);
    return value;
  }
  public Scope put(String name, Value value) {
    table.put(name, value);
    return this;
  }
  public Scope put(FunctionValue f) {
    table.put(f.name, f);
    return this;
  }
  public Scope put(ClassValue c) {
    table.put(c.name, c);
    return this;
  }
}

/// As you can see, this class doesn't actually lex anything.
/// It is really just meant to mirror the Python Lexer class I wrote.
/// Its sole purpose here is for generating useful error messages.
public static final class Lexer {
  public final String string;
  public final String filespec;
  public final Token[] tokens;
  public Lexer(String string, String filespec, int... ii) {
    this.string = string;
    this.filespec = filespec;
    tokens = new Token[ii.length];
    for (int i = 0; i < ii.length; i++)
      tokens[i] = new Token(this, ii[i]);
  }
}

public static final class Token {
  public final Lexer lexer;
  public final int i;
  public Token(Lexer lexer, int i) {
    this.lexer = lexer;
    this.i = i;
  }
}

public static final class ReturnException extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final Value value;
  public ReturnException(Value value) {
    this.value = value;
  }
}
public static final class BreakException extends RuntimeException {
  public static final long serialVersionUID = 42L;
}
public static final class ContinueException extends RuntimeException {
  public static final long serialVersionUID = 42L;
}

public static final class Context {
  // Exception flag.
  // I use this for both control flow and actual exception handling.
  // There are a couple reasons:
  //   1) A lot of modern programming languages use the 'zero-cost' approach,
  //      which means that there is no cost when no exceptions are thrown,
  //      but costs a lot when there are exceptions thrown.
  //      I want to throw a lot of exceptions in my language without having to
  //      worry about performance. Some would argue using exceptions in this
  //      way as a control flow mechanism is bad, but humbug to them.
  //   2) Apaprently Java's 'throw' spends a lot of time poulating
  //      the stack trace according to some article I read online. Which I
  //      imagine is linear in the depth of the stack. I could make it
  //      constant time (with higher constant factor) by using a linked list.
  public boolean exc = false;

  // Some quick hacks for break/continue/return.
  // These are meant to be very special cases of exceptions and so if any of
  // these are true, 'exc' should also be true.
  public boolean br = false, cont = false, ret = false;

  // The resulting value on success, or return value if ret is true.
  public Value value = null;

  // The scope to use to evaluate things with.
  public Scope scope;

  public Context(Scope scope) {
    this.scope = scope;
  }
}

public static abstract class Ast {
  public final Token token;
  public Ast(Token token) {
    this.token = token;
  }
  public abstract void eval(Context c);
}

public static final class StringAst extends Ast {
  public final String value;
  public StringAst(Token token, String value) {
    super(token);
    this.value = value;
  }
  public void eval(Context c) {
    c.value = new StringValue(value);
  }
}

public static final class NumberAst extends Ast {
  public final double value;
  public NumberAst(Token token, double value) {
    super(token);
    this.value = value;
  }
  public void eval(Context c) {
    c.value = new NumberValue(value);
  }
}

public static final class NameAst extends Ast {
  public final String name;
  public NameAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public void eval(Context c) {
    if ((c.value = c.scope.getOrNull(name)) == null) {
      c.exc = true;
      c.value = new StringValue("Name '" + name + "' is not defined");
    }
  }
}

public static final class AssignAst extends Ast {
  public final String name;
  public final Ast expr;
  public AssignAst(Token token, String name, Ast expr) {
    super(token);
    this.name = name;
    this.expr = expr;
  }
  public void eval(Context c) {
    expr.eval(c);
    if (!c.exc)
      c.scope.put(name, c.value);
  }
}

public static final class CallAst extends Ast {
  public final Ast f;
  public final Ast[] args;
  public final Ast vararg; // may be null
  public CallAst(Token token, Ast f, Ast[] args, Ast vararg) {
    super(token);
    this.f = f;
    this.args = args;
    this.vararg = vararg;
  }
  public void eval(Context c) {
    this.f.eval(c);
    if (c.exc)
      return;
    Value f = c.value;

    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < this.args.length; i++) {
      this.args[i].eval(c);
      if (c.exc)
        return;
      args.add(c.value);
    }

    if (this.vararg != null) {
      this.vararg.eval(c);
      if (c.exc)
        return;

      Value vararg = c.value;
      if (!(vararg instanceof ListValue)) {
        c.exc = true;
        c.value = new StringValue(
            "Expected List for vararg but found: " + vararg.getType().name);
        return;
      }

      ArrayList<Value> va = ((ListValue) vararg).value;
      for (int i = 0; i < va.size(); i++)
        args.add(va.get(i));
    }

    f.call(c, args);
  }
}

public static final class GetAttrAst extends Ast {
  public final Ast expr;
  public final String attr;
  public GetAttrAst(Token token, Ast expr, String attr) {
    super(token);
    this.expr = expr;
    this.attr = attr;
  }
  public void eval(Context c) {
    expr.eval(c);
    if (c.exc)
      return;

    Value owner = c.value;

    c.value = owner.getOrNull(attr);

    if (c.value == null) {
      c.exc = true;
      c.value = new StringValue("Couldn't get attribute '" + attr + "'");
    }
  }
}

public static final class SetAttrAst extends Ast {
  public final Ast expr;
  public final String attr;
  public final Ast val;
  public SetAttrAst(Token token, Ast expr, String attr, Ast val) {
    super(token);
    this.expr = expr;
    this.attr = attr;
    this.val = val;
  }
  public void eval(Context c) {
    expr.eval(c);
    if (c.exc)
      return;
    Value owner = c.value;

    this.val.eval(c);
    if (c.exc)
      return;
    Value val = c.value;

    owner.put(attr, val);
  }
}

public static final class FuncAst extends Ast {
  public final String name; // may be null
  public final String[] args;
  public final String vararg; // may be null
  public final Ast body;
  public FuncAst(
      Token token, String name, String[] args, String vararg, Ast body) {
    super(token);
    this.name = name;
    this.args = args;
    this.vararg = vararg;
    this.body = body;
  }
  public void eval(Context c) {
    c.value = new UserFunctionValue(c.scope, name, args, vararg, body);

    if (name != null)
      c.scope.put(name, c.value);
  }
}

public static final class ClassAst extends Ast {
  public final String name; // may be null
  public final Ast[] bases;
  public final Ast varbase; // may be null
  public final Ast body;
  public ClassAst(
      Token token, String name, Ast[] bases, Ast varbase, Ast body) {
    super(token);
    this.name = name;
    this.bases = bases;
    this.varbase = varbase;
    this.body = body;
  }
  public void eval(Context c) {
    ArrayList<Value> bases = new ArrayList<Value>();
    for (int i = 0; i < this.bases.length; i++) {
      this.bases[i].eval(c);
      if (c.exc)
        return;
      bases.add(c.value);
    }

    if (this.varbase != null) {
      this.varbase.eval(c);
      if (c.exc)
        return;
      Value varbase = c.value;
      if (!(varbase instanceof ListValue)) {
        c.exc = true;
        c.value = new StringValue(
            "Expected List for varbase but found " + varbase.getType().name);
        return;
      }
      ArrayList<Value> vb = ((ListValue) varbase).value;
      for (int i = 0; i < vb.size(); i++)
        bases.add(vb.get(i));
    }
    if (bases.size() == 0)
      bases.add(classObject);

    Scope scope = c.scope; // push scope
    c.scope = new Scope(scope);

    body.eval(c);
    if (c.exc)
      return;
    ClassValue cv = new ClassValue(name, bases, c.scope.table);
    if (name != null)
      scope.put(name, cv);

    c.value = cv;
    c.scope = scope; // pop scope
  }
}

public static final class ReturnAst extends Ast {
  public final Ast expr; // may be null
  public ReturnAst(Token token, Ast expr) {
    super(token);
    this.expr = expr;
  }
  public void eval(Context c) {
    expr.eval(c);
    if (c.exc)
      return;

    c.exc = c.ret = true;
  }
}

public static final class BreakAst extends Ast {
  public BreakAst(Token token) {
    super(token);
  }
  public void eval(Context c) {
    c.exc = c.br = true;
  }
}

public static final class ContinueAst extends Ast {
  public ContinueAst(Token token) {
    super(token);
  }
  public void eval(Context c) {
    c.exc = c.cont = true;
  }
}

public static final class WhileAst extends Ast {
  public final Ast cond;
  public final Ast body;
  public WhileAst(Token token, Ast cond, Ast body) {
    super(token);
    this.cond = cond;
    this.body = body;
  }
  public void eval(Context c) {
    while (true) {
      cond.eval(c);
      if (c.exc)
        return;

      if (!c.value.isTruthy())
        break;

      body.eval(c);

      if (c.br) {
        c.exc = c.br = false;
        break;
      }

      if (c.cont) {
        c.exc = c.cont = false;
        continue;
      }

      if (c.exc)
        return;
    }
  }
}

public static final class IfAst extends Ast {
  public final Ast cond;
  public final Ast body;
  public final Ast other; // may be null
  public IfAst(Token token, Ast cond, Ast body, Ast other) {
    super(token);
    this.cond = cond;
    this.body = body;
    this.other = other;
  }
  public void eval(Context c) {
    cond.eval(c);
    if (c.exc)
      return;

    if (c.value.isTruthy())
      body.eval(c);
    else
      other.eval(c);
  }
}

public static final class BlockAst extends Ast {
  public final Ast[] exprs;
  public BlockAst(Token token, Ast[] exprs) {
    super(token);
    this.exprs = exprs;
  }
  public void eval(Context c) {
    for (int i = 0; i < exprs.length; i++) {
      exprs[i].eval(c);
      if (c.exc)
        return;
    }
  }
}

public static final class NotAst extends Ast {
  public final Ast expr;
  public NotAst(Token token, Ast expr) {
    super(token);
    this.expr = expr;
  }
  public void eval(Context c) {
    expr.eval(c);
    if (c.exc)
      return;
    c.value = c.value.isTruthy() ? falseValue : trueValue;
  }
}

public static final class OrAst extends Ast {
  public final Ast left;
  public final Ast right;
  public OrAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public void eval(Context c) {
    this.left.eval(c);
    if (c.exc)
      return;

    Value left = c.value;
    if (!left.isTruthy())
      this.right.eval(c);
  }
}

public static final class AndAst extends Ast {
  public final Ast left;
  public final Ast right;
  public AndAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public void eval(Context c) {
    this.left.eval(c);
    if (c.exc)
      return;

    Value left = c.value;
    if (left.isTruthy())
      this.right.eval(c);
  }
}

public static final class ModuleAst extends Ast {
  public final BlockAst expr;
  public ModuleAst(Token token, BlockAst expr) {
    super(token);
    this.expr = expr;
  }
  public void eval(Context c) {
    expr.eval(c);
  }
}

public static final class IsAst extends Ast {
  public final Ast left;
  public final Ast right;
  public IsAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public void eval(Context c) {
    this.left.eval(c);
    if (c.exc)
      return;

    Value left = c.value;

    this.right.eval(c);
    if (c.exc)
      return;

    Value right = c.value;

    c.value = left == right ? trueValue : falseValue;
  }
}

// Unsynchronized stack (java.util.Stack is synchronized)
public static final class Stack<T> {
  public final ArrayList<T> buffer = new ArrayList<T>();
  public void push(T value) {
    buffer.add(value);
  }
  public T pop() {
    return buffer.remove(buffer.size()-1);
  }
  public T get(int i) {
    return buffer.get(i);
  }
}

}
