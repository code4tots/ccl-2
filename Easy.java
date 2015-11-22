import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

// This top level class exists just for namespacing purposes.
public class Easy {

  static public void main(String[] args) {
    if (args.length == 1)
      eval(readFile(args[0]));
    else
      System.out.println("Usage java Easy <filename>");
  }

  static public String readFile(String filename) {
    BufferedReader reader;
    String line = null;
    StringBuilder sb = new StringBuilder();
    String separator = System.getProperty("line.separator");

    try {
      reader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append(separator);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return sb.toString();
  }

  static public Value eval(String program) {
    return new Parser(program).parse().eval(new Scope(BUILTIN_SCOPE));
  }

// constants
public static final BoolValue trueValue = BoolValue.trueValue;
public static final BoolValue falseValue = BoolValue.falseValue;
public static final NilValue nil = NilValue.nil;

// TODO: User defined metaclasses.
// But only if I can find a really clean, easy and elegant way to do it.
// It should be more elegant than Python's metaclass mechanism.

// Root classes.
static public ClassValue classObject = new ClassValue("Object", null);
static public ClassValue classBuiltinObject =
    new ClassValue("BuiltinObject", classObject);
static public UserClassValue classUserObject =
    new UserClassValue("UserObject", classObject);

// metaclasses
static public ClassValue classClass =
    new ClassValue("Class", classBuiltinObject);
static public ClassValue classBuiltinClass =
    new ClassValue("BuiltinClass", classClass);
static public ClassValue classUserClass =
    new ClassValue("UserClass", classClass);

// Other builtin classes.
static public ClassValue classNil = new ClassValue("Nil", classBuiltinObject);
static public ClassValue classBool =
    new ClassValue("Bool", classBuiltinObject);
static public ClassValue classNumber =
    new ClassValue("Number", classBuiltinObject);
static public ClassValue classString =
    new ClassValue("String", classBuiltinObject);
static public ClassValue classList =
    new ClassValue("List", classBuiltinObject);
static public ClassValue classFunction =
    new ClassValue("Function", classBuiltinObject);

static public final Scope BUILTIN_SCOPE = new Scope(null);

// Fill in scope and class with variables and methods.
static {
  BUILTIN_SCOPE
      .put("nil", nil)
      .put("true", trueValue)
      .put("false", falseValue)
      .put("Object", classUserObject) // TODO: Think about this.
      .put("Class", classClass)
      .put("Nil", classNil)
      .put("Number", classNumber)
      .put("String", classString)
      .put("List", classList)
      .put("print", new BuiltinFunctionValue("print") {
        public Value call(ArrayList<Value> args) {
          Value last = nil;
          for (int i = 0; i < args.size(); i++) {
            if (i > 0)
              System.out.print(" ");
            System.out.print(args.get(i));
            last = args.get(i);
          }
          System.out.println();
          return args.get(args.size() - 1);
        }
      })
      .put("assert", new BuiltinFunctionValue("assert") {
        public Value call(ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{null, null});
          if (!args.get(0).isTruthy()) {
            System.out.println("Assertion failed: " + args.get(1).toString());
            System.exit(1);
          }
          return nil;
        }
      });

  classObject
      .put("__class__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{});
          return owner.getType();
        }
      })
      .put("__ne__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{null});
          return owner.equals(args.get(0)) ? falseValue : trueValue;
        }
      });

  classBuiltinObject
      .put("__eq__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{null});
          return owner.equals(args.get(0)) ? trueValue : falseValue;
        }
      })
      .put("__str__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{});
          return new StringValue(owner.toString());
        }
      });

  classBuiltinClass
      .put("__call__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          return ((FunctionValue) owner.getAttribute("__new__")).call(args);
        }
      });

  classUserClass
      .put("__call__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          Value value = new UserObjectValue((ClassValue) owner);
          value.callMethod("__init__", args);
          return value;
        }
      });

  classUserObject
      .put("__init__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{});
          return nil;
        }
      })
      .put("__str__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{});
          return new StringValue(
              "<" + owner.getType().name + " instance>");
        }
      })
      .put("__eq__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{null});
          return owner == args.get(0) ? trueValue : falseValue;
        }
      });

  classNumber
      .put("__add__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{classNumber});
          Double a = ((NumberValue) owner).value;
          Double b = ((NumberValue) args.get(0)).value;
          return new NumberValue(a + b);
        }
      })
      .put("__sub__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{classNumber});
          Double a = ((NumberValue) owner).value;
          Double b = ((NumberValue) args.get(0)).value;
          return new NumberValue(a - b);
        }
      })
      .put("__neg__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{});
          Double a = ((NumberValue) owner).value;
          return new NumberValue(-a);
        }
      });

  classString
      .put(new BuiltinFunctionValue("__new__") {
        public Value call(ArrayList<Value> args) {
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < args.size(); i++)
            sb.append(args.get(i).toString());
          return new StringValue(sb.toString());
        }
      })
      .put("__add__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{classString});
          String a = ((StringValue) owner).value;
          String b = ((StringValue) args.get(0)).value;
          return new StringValue(a + b);
        }
      });

  classList
      .put("__new__", new BuiltinFunctionValue("__new__") {
        public Value call(ArrayList<Value> args) {
          return new ListValue(args);
        }
      })
      .put("map", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          expectExactArgTypes(args, new ClassValue[]{classFunction});
          ArrayList<Value> value = ((ListValue) owner).value;
          FunctionValue f = (FunctionValue) args.get(0);
          ArrayList<Value> newvals = new ArrayList<Value>();
          for (int i = 0; i < value.size(); i++)
            newvals.add(f.callMethod("__call__", value.get(i)));
          return new ListValue(newvals);
        }
      });

  classFunction
      .put("__call__", new Method() {
        public Value call(Value owner, ArrayList<Value> args) {
          FunctionValue f = (FunctionValue) owner;
          return f.call(args);
        }
      });
}

// scope

static public final class Scope {
  private final Scope parent;
  public final HashMap<String, Value> table;
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
  public Iterator<String> iterator() {
    return table.keySet().iterator();
  }
}

// value

static public void expectArgLen(String methodName, int expected,
                         ArrayList<Value> args) {
  if (args.size() != expected) {
    throw new RuntimeException(Integer.toString(expected) + " " +
                               Integer.toString(args.size()));
  }
}
static public void expectArgLen(int expected, ArrayList<Value> args) {
  expectArgLen("_", expected, args);
}
static public void expectAtLeastArgLen(int expected, ArrayList<Value> args) {
  if (args.size() < expected) {
    throw new RuntimeException(Integer.toString(expected) + " " +
                               Integer.toString(args.size()));
  }
}
static public void expectArgTypes(
    ArrayList<Value> args, ClassValue[] expected) {
  if (args.size() < expected.length)
    throw new RuntimeException(
        "Expected at least " + Integer.toString(expected.length) +
        " arguments but found " + Integer.toString(args.size()) +
        " arguments.");
  for (int i = 0; i < expected.length; i++) {
    ClassValue cls = expected[i];
    if (cls == null)
      continue;
    if (!args.get(i).getType().isSubtypeOf(cls))
      throw new RuntimeException(
          Integer.toString(i) + " " +
          args.get(i).getType().toString() + " " +
          cls.toString());
  }
}
static public void expectExactArgTypes(
    ArrayList<Value> args, ClassValue[] expected) {
  if (args.size() != expected.length)
    throw new RuntimeException(
        "Expected " + Integer.toString(expected.length) + " arguments " +
        "but found " + Integer.toString(args.size()) + " arguments.");
  expectArgTypes(args, expected);
}

static public abstract class Value extends Easy {
  public final HashMap<String, Value> attributes;
  public Value() { this(new HashMap<String, Value>()); }
  public Value(HashMap<String, Value> attrs) { attributes = attrs; }
  public abstract ClassValue getType(); // getClass is already taken by Java.
  public abstract boolean isTruthy();
  public abstract boolean equals(Value value);
  public final Value getAttribute(String name) {
    Value value = get(name);
    if (value == null) {
      throw new RuntimeException(getType().name + "." + name);
    }
    return value;
  }
  public final void setAttribute(String name, Value value) { put(name, value); }
  public Value get(String name) { return attributes.get(name); }
  public Value put(String name, Value value) {
    attributes.put(name, value);
    return this;
  }
  public final Value callMethod(String name, ArrayList<Value> args) {
    return getType().getMethod(name).call(this, args);
  }
  public Value callMethod(String name, Value... args) {
    ArrayList<Value> arglist = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      arglist.add(args[i]);
    return callMethod(name, arglist);
  }
  public boolean equals(Object value) {
    return value instanceof Value && equals((Value) value);
  }
}

static public abstract class Method extends Easy {
  public abstract Value call(Value owner, ArrayList<Value> args);
}

static public class ClassValue extends Value {
  public final String name;
  public final ClassValue parent; // TODO: Implement multiple inheritance.
  public final HashMap<String, Method> methods;
  public ClassValue getType() { return classBuiltinClass; }
  public boolean isTruthy() { return true; }
  private ClassValue(
      String name, ClassValue parent, HashMap<String, Method> methods) {
    this.name = name;
    this.parent = parent;
    this.methods = methods;
  }
  public ClassValue(String name, ClassValue parent) {
    this(name, parent, new HashMap<String, Method>());
  }
  public Value get(String name) {
    Value value = attributes.get(name);
    if (value != null)
      return value;
    if (parent != null)
      return parent.get(name);
    return null;
  }
  public boolean equals(Value value) { return this == value; }
  public Method getMethod(String name) {
    Method method = getMethodOrNull(name);
    if (method == null)
      throw new RuntimeException(this.name + "." + name);
    return method;
  }
  public ClassValue put(String name, Value value) {
    attributes.put(name, value);
    return this;
  }
  public ClassValue put(FunctionValue f) {
    attributes.put(f.name, f);
    return this;
  }
  public Method getMethodOrNull(String name) {
    return getMethodOrNull(name, this.name);
  }
  private Method getMethodOrNull(String name, String className) {
    Method method = methods.get(name);
    if (method == null && parent != null)
        return parent.getMethodOrNull(name, className);
    return method;
  }
  public ClassValue put(String name, Method method) {
    methods.put(name, method);
    return this;
  }
  public String toString() {
    return "<class " + name + ">";
  }
  public boolean isSubtypeOf(ClassValue cls) {
    ClassValue c = this;
    while (c != null && c != cls)
      c = c.parent;
    return c != null;
  }
}

static public class UserClassValue extends ClassValue {
  public UserClassValue(String name, ClassValue parent) {
    super(name, parent);
  }
  public ClassValue getType() { return classUserClass; }
}

static public class NilValue extends Value {
  private NilValue() {}
  public ClassValue getType() { return classNil; }
  public boolean isTruthy() { return false; }
  public boolean equals(Value value) {
    return value instanceof NilValue;
  }
  public String toString() {
    return "nil";
  }
  public static final NilValue nil = new NilValue();
}

static public class BoolValue extends Value {
  public boolean value;
  private BoolValue(boolean value) { this.value = value; }
  public ClassValue getType() { return classBool; }
  public boolean isTruthy() { return value; }
  public boolean equals(Value val) {
    return val instanceof BoolValue && ((BoolValue) val).value == value;
  }
  public final int hashCode() {
    return value ? 1 : 0;
  }
  public String toString() {
    return value ? "true" : "false";
  }
  public static final BoolValue trueValue = new BoolValue(true);
  public static final BoolValue falseValue = new BoolValue(false);
}

static public class NumberValue extends Value {
  public final Double value;
  public NumberValue(Double value) {
    // See javadocs about Double.equals
    this.value = (value == -0.0 ? +0.0 : value);
  }
  public ClassValue getType() { return classNumber; }
  public boolean isTruthy() { return !value.equals(0.0); }
  public boolean equals(Value val) {
    return val instanceof NumberValue &&
        ((NumberValue) val).value.equals(value);
  }
  public int hashCode() {
    return value.hashCode();
  }
  public String toString() {
    return value.toString();
  }
}
static public class StringValue extends Value {
  public final String value;
  public StringValue(String value) { this.value = value; }
  public ClassValue getType() { return classString; }
  public boolean isTruthy() { return value.length() != 0; }
  public boolean equals(Value val) {
    return val instanceof StringValue &&
           ((StringValue) val).value.equals(value);
  }
  public String toString() {
    return value;
  }
}

static public class ListValue extends Value {
  public final ArrayList<Value> value;
  public ListValue(ArrayList<Value> value) { this.value = value; }
  public ListValue(Value... values) {
    this(arrayToArrayList(values));
  }
  public ClassValue getType() { return classList; }
  public boolean isTruthy() { return value.size() != 0; }
  public boolean equals(Value val) {
    return val instanceof ListValue && ((ListValue) val).value.equals(value);
  }
  public String toString() { return value.toString(); }
}

static public abstract class FunctionValue extends Value {
  public final String name;
  public ClassValue getType() { return classFunction; }
  public boolean isTruthy() { return true; }
  public boolean equals(Value val) { return this == val; }
  public abstract Value call(ArrayList<Value> args);
  public FunctionValue(String name) {
    this.name = name;
  }
}

static public abstract class BuiltinFunctionValue extends FunctionValue {
  public BuiltinFunctionValue(String name) { super(name); }
  public String toString() { return "<builtin function " + name + ">"; }
}

static public final class UserFunctionValue extends FunctionValue {
  public final Scope parentScope;
  public final Ast body;
  public final ArrayList<String> argNames;
  public final String vararg;
  public UserFunctionValue(
      String name, Scope parentScope, ArrayList<String> argNames,
      String vararg, Ast body) {
    super(name);
    this.parentScope = parentScope;
    this.body = body;
    this.argNames = argNames;
    this.vararg = vararg;
  }
  public String toString() {
    return "<function " + name + ">";
  }
  public void verifyArguments(ArrayList<Value> args) {
    if (vararg == null)
      expectArgLen(argNames.size(), args);
    else
      expectAtLeastArgLen(argNames.size(), args);
  }
  public Scope getNewScopeWithArguments(ArrayList<Value> args) {
    verifyArguments(args);
    Scope scope = new Scope(parentScope);
    int i;
    for (i = 0; i < argNames.size(); i++) {
      scope.put(argNames.get(i), args.get(i));
    }

    if (vararg != null) {
      ArrayList<Value> rest = new ArrayList<Value>();
      for (; i < args.size(); i++)
        rest.add(args.get(i));
      scope.put(vararg, new ListValue(rest));
    }
    return scope;
  }
  public Value call(ArrayList<Value> args) {
    return body.eval(getNewScopeWithArguments(args));
  }
  // TODO: Refactor and clean this up.
  public Method toMethod() {
    return new Method() {
      public Value call(Value owner, ArrayList<Value> args) {
        Scope scope = getNewScopeWithArguments(args);
        scope.put("self", owner);
        return body.eval(scope);
      }
    };
  }
}

static public final class UserObjectValue extends Value {
  public final ClassValue type;
  public UserObjectValue(ClassValue type) { this.type = type; }
  public ClassValue getType() { return type; }
  public boolean equals(Value value) {
    return callMethod("__eq__", value).isTruthy();
  }
  public boolean isTruthy() { return callMethod("__bool__").isTruthy(); }
  public String toString() {
    Value value = callMethod("__str__");
    if (!(value instanceof StringValue))
      throw new RuntimeException(
          "Expected String type but found " + value.getType().toString());
    return value.toString();
  }
}

// ast

static public Ast[] makeAstArray(Ast... items) { return items; }


static public abstract class Ast extends Easy {
  // TODO: Add a Token field in Ast, so that
  // error messages can know where a given Ast came from.

  public abstract Ast[] children();
  // I think the right thing to do would be to have a separate
  // evaluator class that walks the ast tree, but that would be
  // so much work. Ugh. Java.
  public abstract Value eval(Scope scope);
}

static public class NumberAst extends Ast {
  public final NumberValue value;
  public NumberAst(NumberValue value) { this.value = value; }
  public Value eval(Scope scope) { return value; }
  public Ast[] children() { return makeAstArray(); }
}

static public class StringAst extends Ast {
  public final StringValue value;
  public StringAst(StringValue value) { this.value = value; }
  public Value eval(Scope scope) { return value; }
  public Ast[] children() { return makeAstArray(); }
}

static public class BlockAst extends Ast {
  public final ArrayList<Ast> asts;
  public BlockAst(ArrayList<Ast> asts) { this.asts = asts; }
  public Value eval(Scope scope) {
    Value last = nil;
    for (int i = 0; i < asts.size(); i++)
      last = asts.get(i).eval(scope);
    return last;
  }
  public Ast[] children() { return asts.toArray(new Ast[asts.size()]); }
}

static public class NameAst extends Ast {
  public final String name;
  public NameAst(String name) { this.name = name; }
  public Value eval(Scope scope) { return scope.get(name); }
  public Ast[] children() { return makeAstArray(); }
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
  public Ast[] children() { return makeAstArray(valueAst); }
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
  public Ast[] children() { return makeAstArray(ownerAst); }
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
  public Ast[] children() { return makeAstArray(ownerAst, valueAst); }
}

static public ArrayList<Ast> arrayToArrayList(Ast... args) {
  ArrayList<Ast> arglist = new ArrayList<Ast>();
  for (int i = 0; i < args.length; i++)
    arglist.add(args[i]);
  return arglist;
}

static public ArrayList<Value> arrayToArrayList(Value... args) {
  ArrayList<Value> arglist = new ArrayList<Value>();
  for (int i = 0; i < args.length; i++)
    arglist.add(args[i]);
  return arglist;
}

static public class CallMethodAst extends Ast {
  public final Ast ownerAst;
  public final String methodName;
  public final ArrayList<Ast> argumentAsts;
  public final Ast vararg;
  public CallMethodAst(Ast ownerAst, String methodName,
                       ArrayList<Ast> argumentAsts,
                       Ast vararg) {
    this.ownerAst = ownerAst;
    this.methodName = methodName;
    this.argumentAsts = argumentAsts;
    this.vararg = vararg;
  }
  public CallMethodAst(Ast owner, String name, ArrayList<Ast> args) {
    this(owner, name, args, null);
  }
  public CallMethodAst(Ast owner, String name, Ast... args) {
    this(owner, name, arrayToArrayList(args));
  }
  public Value eval(Scope scope) {
    Value owner = ownerAst.eval(scope);
    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < argumentAsts.size(); i++)
      args.add(argumentAsts.get(i).eval(scope));
    if (vararg != null) {
      Value va = vararg.eval(scope);
      if (!(va instanceof ListValue))
        throw new RuntimeException();
      ArrayList<Value> al = ((ListValue) va).value;
      for (int i = 0; i < al.size(); i++)
        args.add(al.get(i));
    }
    return owner.callMethod(methodName, args);
  }
  public Ast[] children() {
    Ast[] items = new Ast[
        argumentAsts.size() + (vararg == null ? 1 : 2)];
    items[0] = ownerAst;
    for (int i = 0; i < argumentAsts.size(); i++)
      items[i+1] = argumentAsts.get(i);
    if (vararg != null)
      items[argumentAsts.size()+1] = vararg;
    return items;
  }
}

static public class AndAst extends Ast {
  public final Ast left, right;
  public AndAst(Ast left, Ast right) {
    this.left = left;
    this.right = right;
  }
  public Value eval(Scope scope) {
    Value result = left.eval(scope);
    return !result.isTruthy() ? result : right.eval(scope);
  }
  public Ast[] children() {
    return makeAstArray(left, right);
  }
}

static public class OrAst extends Ast {
  public final Ast left, right;
  public OrAst(Ast left, Ast right) {
    this.left = left;
    this.right = right;
  }
  public Value eval(Scope scope) {
    Value result = left.eval(scope);
    return result.isTruthy() ? result : right.eval(scope);
  }
  public Ast[] children() {
    return makeAstArray(left, right);
  }
}

static public class NotAst extends Ast {
  public final Ast expr;
  public NotAst(Ast expr) {
    this.expr = expr;
  }
  public Value eval(Scope scope) {
    return expr.eval(scope).isTruthy() ? falseValue : trueValue;
  }
  public Ast[] children() {
    return makeAstArray(expr);
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
    return condition.eval(scope).isTruthy() ?
        body.eval(scope) : otherwise.eval(scope);
  }
  public Ast[] children() {
    return makeAstArray(condition, body, otherwise);
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
  public Ast[] children() {
    return makeAstArray(condition, body);
  }
}

static public class UserFunctionAst extends Ast {
  public final String name;
  public final ArrayList<String> argNames;
  public final String vararg;
  public final Ast body;
  public UserFunctionAst(
      String name, ArrayList<String> argNames, String vararg, Ast body) {
    this.name = name;
    this.argNames = argNames;
    this.vararg = vararg;
    this.body = body;
  }
  public Value eval(Scope scope) {
    return new UserFunctionValue(name, scope, argNames, vararg, body);
  }
  public Ast[] children() {
    return makeAstArray(body);
  }
}

static public class UserClassAst extends Ast {
  public final String name;
  public final Ast baseAst; // TODO: multiple inheritance
  public final Ast body;
  public UserClassAst(String name, Ast baseAst, Ast body) {
    this.name = name;
    this.baseAst = baseAst;
    this.body = body;
  }
  public Ast[] children() {
    return makeAstArray(body);
  }
  public Value eval(Scope scope) {
    ClassValue cls = new UserClassValue(name, (ClassValue) baseAst.eval(scope));
    // Effectively classScope is the class's private scope.
    Scope classScope = new Scope(scope);
    classScope.put("self", cls);
    body.eval(classScope);
    Iterator<String> it = classScope.iterator();
    while (it.hasNext()) {
      String key = it.next();
      Value val = classScope.get(key);
      // TODO: This feels like a hack. Refactor. Or at least think about it.
      if (val instanceof UserFunctionValue) {
        cls.put(key, ((UserFunctionValue) val).toMethod());
      }
    }
    scope.put(name, cls);
    return cls;
  }
}

// lexer

static public final String[] SYMBOLS = {
  "(", ")", "[", "]", "{", "}", ";", ",", ".", "\\",
  "+", "-", "*", "/", "%", "=",
  "==", "<", ">", "<=", ">=", "!="
};
static public final String[] KEYWORDS = {
  "not", "and", "or", "def", "class",
  "while", "break", "continue", "if", "else", "return"
};

static public final class Token {
  // I think the right thing to do is to have a separate class that contains
  // source location data. But ugh. I don't wanna do more work.
  public final Lexer lexer;
  public final int location;
  public final String type;
  public final Object value;
  public Token(Lexer lexer, int location, String type, Object value) {
    this.lexer = lexer;
    this.location = location;
    this.type = type;
    this.value = value;
  }
}

static public final class Lexer {
  public final String text;
  private int a, b;
  private Token lookahead;
  private boolean doneFlag;
  public Lexer(String text) {
    this.text = text;
    a = 0;
    lookahead = extractNext();
  }
  public Token makeToken(String type, Object value) {
    Token token = new Token(this, a, type, value);
    a = b;
    return token;
  }
  public Token makeToken(String type) { return makeToken(type, null); }
  public boolean done() { return doneFlag; }
  public String sliceText() { return text.substring(a, b); }
  public void skipSpacesAndComments() {
    while (true) {
      while (b < text.length() && Character.isWhitespace(text.charAt(b)))
        b++;
      if (b < text.length() && text.charAt(b) == '#')
        while (b < text.length() && text.charAt(b) != '\n')
          b++;
      else
        break;
    }
    a = b;
  }
  public boolean isIdentifierCharacter(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }
  public Token peek() { return lookahead; }
  public Token next() {
    Token token = lookahead;
    lookahead = extractNext();
    return token;
  }
  private Token extractNext() {
    skipSpacesAndComments();

    if (doneFlag)
      throw new RuntimeException();

    if (a >= text.length()) {
      doneFlag = true;
      return makeToken("EOF");
    }

    // Lex numbers.
    {
      boolean seenDigit = false;
      char c = text.charAt(b);
      if (c == '+' || c == '-') 
        b++;
      while (b < text.length() && Character.isDigit(text.charAt(b))) {
        seenDigit = true;
        b++;
      }
      if (b < text.length() && text.charAt(b) == '.')
        b++;
      while (b < text.length() && Character.isDigit(text.charAt(b))) {
        seenDigit = true;
        b++;
      }

      if (seenDigit)
        return makeToken("NUM", Double.valueOf(sliceText()));
      else
        b = a;
    }

    // Lex strings.
    if (text.startsWith("\"", b) || text.startsWith("r\"", b) ||
        text.startsWith("r'", b) || text.startsWith("'", b)) {
      boolean raw = false;
      if (text.charAt(b) == 'r') {
        b++;
        raw = true;
      }

      int qlen =
          (text.startsWith("'''", b) || text.startsWith("\"\"\"", b)) ? 3 : 1;
      String quote = text.substring(b, b + qlen);
      b += qlen;
      StringBuilder sb = new StringBuilder();

      while (!text.startsWith(quote, b)) {
        if (b >= text.length())
          throw new RuntimeException();

        if (!raw && text.charAt(b) == '\\') {
          b++;
          if (b >= text.length())
            throw new RuntimeException();
          switch(text.charAt(b++)) {
          case 'n': sb.append('\n'); break;
          case 't': sb.append('\t'); break;
          case '"': sb.append('"'); break;
          case '\'': sb.append('\''); break;
          default:
            throw new RuntimeException(Character.toString(text.charAt(b-1)));
          }
        } else sb.append(text.charAt(b++));
      }

      b += qlen;
      return makeToken("STR", sb.toString());
    }

    // Lex identifiers.
    while (b < text.length() && isIdentifierCharacter(text.charAt(b)))
      b++;

    if (a != b) {
      String value = sliceText();
      for (int i = 0; i < KEYWORDS.length; i++)
        if (value.equals(KEYWORDS[i]))
          return makeToken(value);

      return makeToken("ID", value);
    }

    // Lex symbols.
    {
      String matchedSymbol = "";
      for (int i = 0; i < SYMBOLS.length; i++) {
        String symbol = SYMBOLS[i];
        if (text.startsWith(symbol, a) &&
            symbol.length() > matchedSymbol.length())
          matchedSymbol = symbol;
      }
      if (!matchedSymbol.equals("")) {
        b += matchedSymbol.length();
        return makeToken(sliceText());
      }
    }

    while (b < text.length() && !Character.isWhitespace(text.charAt(b)))
      b++;
    throw new RuntimeException(sliceText());
  }
}

static public final class Parser {
  public final Lexer lexer;
  public Parser(String text) {
    lexer = new Lexer(text);
  }

  public boolean at(String type) {
    return lexer.peek().type.equals(type);
  }

  public boolean consume(String type) {
    if (at(type)) {
      lexer.next();
      return true;
    }
    return false;
  }

  public Token expect(String type) {
    if (!at(type))
      throw new RuntimeException(type + " " + lexer.peek().type);
    return lexer.next();
  }

  public Ast parse() {
    return parseAll();
  }

  public Ast parseAll() {
    ArrayList<Ast> asts = new ArrayList<Ast>();
    while (!lexer.done())
      asts.add(parseExpression());
    return new BlockAst(asts);
  }

  public Ast parseExpression() {
    return parseComparisonExpression();
  }

  public Ast parseComparisonExpression() {
    Ast expr = parseOrExpression();
    while (true) {

      if (consume("==")) {
        expr = new CallMethodAst(expr, "__eq__", parseOrExpression());
        continue;
      }

      if (consume("!=")) {
        expr = new CallMethodAst(expr, "__ne__", parseOrExpression());
        continue;
      }

      break;
    }

    return expr;
  }

  public Ast parseOrExpression() {
    Ast expr = parseAndExpression();
    while (true) {

      if (consume("or")) {
        expr = new OrAst(expr, parseAndExpression());
        continue;
      }

      break;
    }

    return expr;
  }

  public Ast parseAndExpression() {
    Ast expr = parseAdditiveExpression();
    while (true) {

      if (consume("and")) {
        expr = new AndAst(expr, parseAdditiveExpression());
        continue;
      }

      break;
    }

    return expr;
  }

  public Ast parseAdditiveExpression() {
    Ast expr = parseMultiplicativeExpression();
    while (true) {

      if (consume("+")) {
        expr = new CallMethodAst(
            expr, "__add__", parseMultiplicativeExpression());
        continue;
      }

      if (consume("-")) {
        expr = new CallMethodAst(
            expr, "__sub__", parseMultiplicativeExpression());
        continue;
      }

      break;
    }

    return expr;
  }

  public Ast parseMultiplicativeExpression() {
    Ast expr = parsePrefixExpression();
    while (true) {

      if (consume("*")) {
        expr = new CallMethodAst(expr, "__mul__", parsePrefixExpression());
        continue;
      }

      if (consume("/")) {
        expr = new CallMethodAst(expr, "__div__", parsePrefixExpression());
        continue;
      }

      if (consume("%")) {
        expr = new CallMethodAst(expr, "__mod__", parsePrefixExpression());
        continue;
      }

      break;
    }

    return expr;
  }

  public Ast parsePrefixExpression() {
    if (consume("+"))
      return new CallMethodAst(parsePostfixExpression(), "__pos__");

    if (consume("-"))
      return new CallMethodAst(parsePostfixExpression(), "__neg__");

    if (consume("not"))
      return new NotAst(parsePrefixExpression());

    return parsePostfixExpression();
  }

  public Ast parsePostfixExpression() {
    Ast expr = parsePrimaryExpression();
    while (true) {

      if (at("[")) {
        expr = parseRestOfMethodCall(expr, "__call__");
        continue;
      }

      if (consume(".")) {
        String attr = (String) expect("ID").value;

        if (at("[")) {
          expr = parseRestOfMethodCall(expr, attr);
          continue;
        }

        if (consume("=")) {
          expr = new SetAttributeAst(expr, attr, parseExpression());
          continue;
        }

        expr = new GetAttributeAst(expr, attr);
        continue;
      }

      break;
    }
    return expr;
  }

  public Ast parseRestOfMethodCall(Ast owner, String methodName) {
    expect("[");
    ArrayList<Ast> args = new ArrayList<Ast>();
    Ast vararg = null;
    while (!at("]") && !at("*")) {
      args.add(parseExpression());
      consume(",");
    }
    if (consume("*"))
      vararg = parseExpression();
    expect("]");
    return new CallMethodAst(owner, methodName, args, vararg);
  }

  public Ast parsePrimaryExpression() {
    if (consume("(")) {
      Ast expr = parseExpression();
      expect(")");
      return expr;
    }

    if (consume("{")) {
      ArrayList<Ast> exprs = new ArrayList<Ast>();
      while (!consume("}")) {
        exprs.add(parseExpression());
        while (consume(";"));
      }
      return new BlockAst(exprs);
    }

    if (consume("\\")) {
      ArrayList<String> args = new ArrayList<String>();
      String vararg = null;
      while (!at(".") && !at("*")) {
        args.add((String) expect("ID").value);
        consume(",");
      }
      if (consume("*"))
        vararg = (String) expect("ID").value;
      expect(".");
      Ast body = parseExpression();
      return new UserFunctionAst("_", args, vararg, body);
    }

    if (consume("if")) {
      Ast condition = parseExpression();
      Ast body = parseExpression();
      Ast other = new NameAst("nil");
      if (consume("else"))
        other = parseExpression();
      return new IfAst(condition, body, other);
    }

    if (consume("def")) {
      String name = "_";
      if (at("ID"))
        name = (String) expect("ID").value;
      expect("[");
      ArrayList<String> args = new ArrayList<String>();
      String vararg = null;
      while (!at("]") && !at("*")) {
        args.add((String) expect("ID").value);
        consume(",");
      }
      if (consume("*"))
        vararg = (String) expect("ID").value;
      expect("]");
      Ast body = parseExpression();
      return new AssignAst(
          name, new UserFunctionAst(name, args, vararg, body));
    }

    if (consume("class")) {
      String name = (String) expect("ID").value;
      Ast base = new NameAst("Object");
      if (consume("[")) {
        base = parseExpression();
        expect("]"); // TODO: multiple inheritance
      }
      Ast body = parseExpression();
      return new UserClassAst(name, base, body);
    }

    if (at("NUM"))
      return new NumberAst(new NumberValue((Double) expect("NUM").value));

    if (at("STR"))
      return new StringAst(new StringValue((String) expect("STR").value));

    if (at("ID")) {
      String name = (String) expect("ID").value;
      if (consume("="))
        return new AssignAst(name, parseExpression());
      return new NameAst(name);
    }

    throw new RuntimeException(lexer.peek().type);
  }
}

}
