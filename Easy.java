import java.util.Stack;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public abstract class Easy {

public static final NilValue nil = new NilValue();
public static final BoolValue trueValue = new BoolValue(true);
public static final BoolValue falseValue = new BoolValue(false);

public static final ClassValue classObject = new ClassValue("Object");
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
        });
public static final ClassValue classString =
    new ClassValue("String", classObject)
        .put(new BuiltinMethodValue("__add__") {
          public Value call(Value owner, ArrayList<Value> args) {
            expectArglen(1, args);
            return new StringValue(
                owner.getStringValue() + args.get(0).getStringValue());
          }
        });
public static final ClassValue classList =
    new ClassValue("List", classObject);
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

public static Value run(Ast ast) {
  return ast.eval(new Scope(BUILTIN_SCOPE));
}

public static abstract class Value {
  public abstract ClassValue getType();
  public Value call(ArrayList<Value> args) {
    throw new RuntimeException(getClass().getName());
  }
  public Value get(String name) {
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
}

public static final class NilValue extends Value {
  public ClassValue getType() {
    return classNil;
  }
  public boolean isTruthy() {
    return false;
  }
  public String toString() {
    return "nil";
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
  public String toString() {
    return value ? "true" : "false";
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
  public String toString() {
    return value;
  }
  public String getStringValue() {
    return value;
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
  public String toString() {
    if (value == Math.floor(value))
      return Integer.toString((int) value);
    return Double.toString(value);
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
}

public static abstract class FunctionValue extends Value {
  public final String name;
  public FunctionValue(String name) {
    this.name = name;
  }
  public ClassValue getType() {
    return classFunction;
  }
  public boolean isTruthy() {
    return true;
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
    try {
      return body.eval(scope);
    } catch (ReturnException e) {
      return e.value;
    }
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
    try {
      return body.eval(scope);
    } catch (ReturnException e) {
      return e.value;
    }
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

public static final class ClassValue extends Value {
  public final String name;
  public final ArrayList<ClassValue> bases;
  public final HashMap<String, Value> attrs;
  public ClassValue(
      String name, ArrayList<Value> bases, HashMap<String, Value> attrs) {
    this.name = name;
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
  public ClassValue getType() {
    return classClass;
  }
  public boolean isTruthy() {
    return true;
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
  public String toString() {
    return name;
  }
}

public static final class Scope {
  public final HashMap<String, Value> table;
  public final Scope parent;
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
  public final Value value;
  public ReturnException(Value value) {
    this.value = value;
  }
}
public static final class BreakException extends RuntimeException {}
public static final class ContinueException extends RuntimeException {}

public static abstract class Ast {
  public final Token token;
  public Ast(Token token) {
    this.token = token;
  }
  public abstract Value eval(Scope scope);
}

public static final class StringAst extends Ast {
  public final String value;
  public StringAst(Token token, String value) {
    super(token);
    this.value = value;
  }
  public Value eval(Scope scope) {
    return new StringValue(value);
  }
}

public static final class NumberAst extends Ast {
  public final double value;
  public NumberAst(Token token, double value) {
    super(token);
    this.value = value;
  }
  public Value eval(Scope scope) {
    return new NumberValue(value);
  }
}

public static final class NameAst extends Ast {
  public final String name;
  public NameAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public Value eval(Scope scope) {
    return scope.get(name);
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
  public Value eval(Scope scope) {
    Value value = expr.eval(scope);
    scope.put(name, value);
    return value;
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
  public Value eval(Scope scope) {
    Value f = this.f.eval(scope);
    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < this.args.length; i++)
      args.add(this.args[i].eval(scope));
    if (this.vararg != null) {
      Value vararg = this.vararg.eval(scope);
      if (!(vararg instanceof ListValue))
        throw new RuntimeException(vararg.getClass().getName());
      ArrayList<Value> va = ((ListValue) vararg).value;
      for (int i = 0; i < va.size(); i++)
        args.add(va.get(i));
    }
    return f.call(args);
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
  public Value eval(Scope scope) {
    Value owner = expr.eval(scope);
    return owner.get(attr);
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
  public Value eval(Scope scope) {
    Value owner = expr.eval(scope);
    Value val = this.val.eval(scope);
    owner.put(attr, val);
    return val;
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
  public Value eval(Scope scope) {
    return new UserFunctionValue(scope, name, args, vararg, body);
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
  public Value eval(Scope scope) {
    ArrayList<Value> bases = new ArrayList<Value>();
    for (int i = 0; i < this.bases.length; i++)
      bases.add(this.bases[i].eval(scope));
    Value varbase = this.varbase.eval(scope);
    if (!(varbase instanceof ListValue))
      throw new RuntimeException(varbase.getClass().getName());
    ArrayList<Value> vb = ((ListValue) varbase).value;
    for (int i = 0; i < vb.size(); i++)
      bases.add(vb.get(i));
    Scope clsScope = new Scope(scope);
    body.eval(clsScope);
    return new ClassValue(name, bases, clsScope.table);
  }
}

public static final class ReturnAst extends Ast {
  public final Ast expr; // may be null
  public ReturnAst(Token token, Ast expr) {
    super(token);
    this.expr = expr;
  }
  public Value eval(Scope scope) {
    throw new ReturnException(expr.eval(scope));
  }
}

public static final class BreakAst extends Ast {
  public BreakAst(Token token) {
    super(token);
  }
  public Value eval(Scope scope) {
    throw new BreakException();
  }
}

public static final class ContinueAst extends Ast {
  public ContinueAst(Token token) {
    super(token);
  }
  public Value eval(Scope scope) {
    throw new ContinueException();
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
  public Value eval(Scope scope) {
    try {
      while (cond.eval(scope).isTruthy()) {
        try {
          body.eval(scope);
        } catch (ContinueException e) {}
      }
    } catch (BreakException e) {}
    return nil;
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
  public Value eval(Scope scope) {
    return cond.eval(scope).isTruthy() ? body.eval(scope) : other.eval(scope);
  }
}

public static final class BlockAst extends Ast {
  public final Ast[] exprs;
  public BlockAst(Token token, Ast[] exprs) {
    super(token);
    this.exprs = exprs;
  }
  public Value eval(Scope scope) {
    for (int i = 0; i < exprs.length; i++)
      exprs[i].eval(scope);
    return nil;
  }
}

public static final class NotAst extends Ast {
  public final Ast expr;
  public NotAst(Token token, Ast expr) {
    super(token);
    this.expr = expr;
  }
  public Value eval(Scope scope) {
    return expr.eval(scope).isTruthy() ? falseValue : trueValue;
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
  public Value eval(Scope scope) {
    Value left = this.left.eval(scope);
    return left.isTruthy() ? left : this.right.eval(scope);
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
  public Value eval(Scope scope) {
    Value left = this.left.eval(scope);
    return left.isTruthy() ? this.right.eval(scope) : left;
  }
}

public static final class ModuleAst extends Ast {
  public final BlockAst expr;
  public ModuleAst(Token token, BlockAst expr) {
    super(token);
    this.expr = expr;
  }
  public Value eval(Scope scope) {
    return expr.eval(scope);
  }
}

}
