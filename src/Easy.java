import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class Easy {

public static final NilValue nil = new NilValue();
public static final BoolValue trueValue = new BoolValue(true);
public static final BoolValue falseValue = new BoolValue(false);

public static final ClassValue classObject =
    new ClassValue("Object")
        .put(new BuiltinMethodValue("__new__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            Value value = new UserValue((ClassValue) owner);
            Value method = value.getOrNull("__init__");
            if (method == null) {
              c.exc = true;
              c.value = makeException(
                  c.trace,
                  value.getType().name + " has no __init__");
              return;
            }
            method.call(c);
            c.value = value;
          }
        })
        .put(new BuiltinMethodValue("__init__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = nil;
          }
        })
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value =
                new StringValue("<" + owner.getType().name + " instance>");
          }
        })
        .put(new BuiltinMethodValue("__str__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            owner.call(c, "__repr__", args);
          }
        })
        .put(new BuiltinMethodValue("__hash__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = new NumberValue(System.identityHashCode(owner));
          }
        })
        .put(new BuiltinMethodValue("__bool__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = trueValue;
          }
        });
public static final ClassValue classTrace =
    new ClassValue("Trace", classObject)
        .put(new BuiltinFunctionValue("__new__") {
          public void call(Context c, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = c.trace;
          }
        })
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            StringBuilder sb = new StringBuilder();
            for (TraceValue t = (TraceValue) owner; t != null; t = t.next)
              sb.append(t.node.token.getLocationString());
            c.value = new StringValue(sb.toString());
          }
        });
public static final ClassValue classNamedObject =
    new ClassValue("NamedObject", classObject)
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = new StringValue(
                "<" + owner.getType().name + " " +
                ((NamedValue) owner).name + ">");
          }
        });
public static final ClassValue classClass =
    new ClassValue("Class", classNamedObject);
public static final ClassValue classException =
    new ClassValue("Exception", classObject)
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            UserValue v = (UserValue) owner;

            v.get(c, "message");
            if (c.exc) return;
            String message = c.value.getStringValue();

            v.get(c, "trace");
            if (c.exc) return;
            Value t = c.value;

            t.call(c, "__repr__");
            if (c.exc) return;
            String traceStr = c.value.getStringValue();

            c.value = new StringValue(message + "\n" + traceStr);
          }
        });
public static final ClassValue classNil =
    new ClassValue("Nil", classObject)
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = new StringValue("nil");
          }
        })
        .put(new BuiltinMethodValue("__bool__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = falseValue;
          }
        });
public static final ClassValue classBool =
    new ClassValue("Bool", classObject)
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = new StringValue(owner.getBoolValue() ? "true" : "false");
          }
        })
        .put(new BuiltinMethodValue("__bool__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = owner;
          }
        });
public static final ClassValue classNumber =
    new ClassValue("Number", classObject)
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            double val = owner.getNumberValue();
            c.value = new StringValue(
                val == Math.floor(val) ?
                Integer.toString((int) val) : Double.toString(val));
          }
        })
        .put(new BuiltinMethodValue("__bool__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = owner.getNumberValue() != 0 ? trueValue : falseValue;
          }
        })
        .put(new BuiltinMethodValue("__eq__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value =
                (args.get(0).getType() == classNumber) &&
                owner.getNumberValue() == args.get(0).getNumberValue() ?
                trueValue : falseValue;
          }
        })
        .put(new BuiltinMethodValue("__add__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value = new NumberValue(
                owner.getNumberValue() + args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__sub__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value = new NumberValue(
                owner.getNumberValue() - args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__mul__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value = new NumberValue(
                owner.getNumberValue() * args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__div__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value = new NumberValue(
                owner.getNumberValue() / args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("__mod__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value = new NumberValue(
                owner.getNumberValue() % args.get(0).getNumberValue());
          }
        })
        .put(new BuiltinMethodValue("floor") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = new NumberValue(Math.floor(owner.getNumberValue()));
          }
        })
        .put(new BuiltinMethodValue("frac") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            double value = owner.getNumberValue();
            c.value = new NumberValue(value - Math.floor(value));
          }
        });
public static final ClassValue classString =
    new ClassValue("String", classObject)
        .put(new BuiltinMethodValue("__str__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value = owner;
          }
        })
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            String value = owner.getStringValue();
            StringBuilder sb = new StringBuilder();
            sb.append("\"");
            for (int i = 0; i < value.length(); i++) {
              char ch = value.charAt(i);
              switch(ch) {
              case '\n': sb.append("\\n"); break;
              case '"': sb.append("\\\""); break;
              default: sb.append(ch);
              }
            }
            sb.append("\"");
            c.value = new StringValue(sb.toString());
          }
        })
        .put(new BuiltinMethodValue("__bool__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value =
                owner.getStringValue().length() != 0 ? trueValue : falseValue;
          }
        })
        .put(new BuiltinMethodValue("__add__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            c.value = new StringValue(
                owner.getStringValue() + args.get(0).getStringValue());
          }
        })
        .put(new BuiltinMethodValue("__mod__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;
            ArrayList<Value> items = args.get(0).getListValue();
            String[] aa = new String[items.size()];
            for (int i = 0; i < items.size(); i++)
              aa[i] = items.get(i).repr();
            String format = owner.getStringValue();
            c.value = new StringValue(String.format(format, (Object[]) aa));
          }
        })
        .put(new BuiltinMethodValue("__eq__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 1, args)) return;

            c.value =
                args.get(0) instanceof StringValue &&
                owner.getStringValue().equals(args.get(0).getStringValue()) ?
                trueValue : falseValue;
          }
        });
public static final ClassValue classList =
    new ClassValue("List", classObject)
        .put(new BuiltinFunctionValue("__new__") {
          public void call(Context c, ArrayList<Value> args) {
            c.value = new ListValue(args);
          }
        })
        .put(new BuiltinMethodValue("__repr__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            ArrayList<Value> value = owner.getListValue();
            StringBuilder sb = new StringBuilder("List[");
            for (int i = 0; i < value.size(); i++) {
              if (i != 0)
                sb.append(", ");
              value.get(i).call(c, "__repr__");
              if (c.exc) return;
              sb.append(c.value.getStringValue());
            }
            sb.append("]");
            c.value = new StringValue(sb.toString());
          }
        })
        .put(new BuiltinMethodValue("__bool__") {
          public void callm(Context c, Value owner, ArrayList<Value> args) {
            if (expectArglen(c, 0, args)) return;
            c.value =
                owner.getListValue().size() != 0 ? trueValue : falseValue;
          }
        });
public static final ClassValue classMap = new ClassValue("Map", classObject);
public static final ClassValue classFunction =
    new ClassValue("Function", classNamedObject);
public static final ClassValue classModule =
    new ClassValue("Module", classObject);

public static final HashMap<String, Class<?>> moduleCache =
    new HashMap<String, Class<?>>();

public static Class<?> getModuleClass(Context c, String moduleName) {
  Class<?> cls = moduleCache.get(moduleName);
  if (cls == null) {
    try {
      cls = Class.forName("CclModule" + moduleName);
    } catch (ClassNotFoundException e) {
      c.exc = true;
      c.value = makeException(
          c.trace, "Module " + moduleName + " not found");
      return null;
    }
  }
  return cls;
}

public static void importModule(Context c, String moduleName) {
  Class<?> cls = getModuleClass(c, moduleName);
  if (c.exc) return;
  try {
    Method method = cls.getMethod("importModule", Context.class, String.class);
    method.invoke(null, c, moduleName);
  } catch (ReflectiveOperationException e) {
    // This is a real FUBAR, since this is not a CCL exception but a Java
    // exception. For the purposes of debugging, I might as well just
    // dump stack trace so I can debug it more easily.
    e.getCause().printStackTrace();
    throw new RuntimeException(e.getCause());
  }
}

public static void runAndGetValue(Context c, Ast body, String name) {
  Scope oldScope = c.scope;
  try {
    c.scope = new Scope(BUILTIN_SCOPE);

    if (name == null)
      name = "__main__";
    c.scope.put("__name__", new StringValue(name));

    body.eval(c);
    if (c.exc) return;

    c.value = new UserValue(classModule, c.scope.table);
  } finally {
    c.scope = oldScope;
  }
}

public static Scope BUILTIN_SCOPE = new Scope(null)
    .put("nil", nil)
    .put("true", trueValue)
    .put("false", falseValue)
    .put(classObject)
    .put(classClass)
    .put(classNil)
    .put(classBool)
    .put(classNumber)
    .put(classString)
    .put(classList)
    .put(classMap)
    .put(classFunction)
    .put(classTrace)
    .put(new BuiltinFunctionValue("print") {
      public void call(Context c, ArrayList<Value> args) {
        if (expectArglen(c, 1, args)) return;
        System.out.println(args.get(0));
        c.value = args.get(0);
      }
    });

public static boolean expectArglen(
    Context c, int len, ArrayList<Value> args) {
  if (len != args.size()) {
    c.exc = true;
    c.value = makeException(
        c.trace,
        "Expected " + Integer.toString(len) + " args but found " +
        Integer.toString(args.size()));
    return true;
  }
  return false;
}

public static boolean expectArglenStar(
    Context c, int len, ArrayList<Value> args) {
  if (len > args.size()) {
    c.exc = true;
    c.value = makeException(
        c.trace,
        "Expected at least " + Integer.toString(len) + " args but found " +
        Integer.toString(args.size()));
    return true;
  }
  return false;
}

public static UserValue makeException(TraceValue tr, String message) {
  return new UserValue(classException)
      .put("trace", tr)
      .put("message", new StringValue(message));
}

public static UserValue makeException(Value tr, Value message) {
  return new UserValue(classException)
      .put("trace", tr)
      .put("message", message);
}

public static void handleBarrierException(Context c, BarrierException e) {
  // TODO: Figure out how to handle exception while handling a barrier
  // exception.
  Context cc = new Context(null);
  e.e.get(cc, "trace");
  if (cc.exc) throw new RuntimeException("FUBAR");
  TraceValue trace = (TraceValue) cc.value;

  e.e.get(cc, "message");
  if (cc.exc) throw new RuntimeException("FUBAR");
  String message = cc.value.getStringValue();

  c.exc = true;
  c.value = makeException(joinStackTraces(c.trace, trace), message);
}

public static void run(Context c, Ast ast) {
  runAndGetValue(c, ast, null);
  if (c.exc) {
    System.out.println(
        "===================\n" +
        "**** Exception ****\n" +
        "===================\n" + c.value.toString());
  }
}

public static void run(Ast ast) {
  run(new Context(new Scope(BUILTIN_SCOPE)), ast);
}

public static abstract class Value {
  public abstract ClassValue getType();
  public final Value call(ArrayList<Value> args)  {
    throw new RuntimeException(
        getClass().getName() + " Calling this way is outdated");
  }
  public void call(Context c, ArrayList<Value> args) {
    c.exc = true;
    c.value = makeException(
        c.trace,getType().name + " is not callable");
  }
  public final void call(Context c, Value... args) {
    ArrayList<Value> al = new ArrayList<Value>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    call(c, al);
  }
  public final Value callOrThrow(Value... args) {
    Context c = new Context(null);
    call(c, args);
    if (c.exc) throw new BarrierException(c.value);
    return c.value;
  }
  public final Value getOrThrow(String name) {
    Context c = new Context(null);
    get(c, name);
    if (c.exc) throw new BarrierException(c.value);
    return c.value;
  }
  public final void get(Context c, String name) {
    Value value = getOrNull(name);
    if (value == null) {
      c.exc = true;
      c.value = makeException(
          c.trace, "No attr " + name);
    }
    c.value = value;
  }
  public Value getOrNull(String name) {
    Value value = getType().getForInstance(name);
    return value == null ? null : value.bind(this);
  }
  public Value put(String name, Value value) {
    throw new RuntimeException(getClass().getName() + "." + name);
  }
  public Value bind(Value owner) {
    return this;
  }

  // For the sake of consistency, I think all of the following methods should
  // really take a Context argument. However, it is just more convenient this
  // way. And really, these are the sort of operations where you really
  // don't expect an exception unless something really goes wrong.
  // So instead if there is an error here, a BarrierException is thrown.
  public double getNumberValue() {
    throw new BarrierException(makeException(
        null, "Expected a Number but found " + getType().name));
  }
  public String getStringValue() {
    throw new BarrierException(makeException(
        null, "Expected a String but found " + getType().name));
  }
  public ArrayList<Value> getListValue()  {
    throw new BarrierException(makeException(
        null, "Expected a List but found " + getType().name));
  }
  public boolean getBoolValue() {
    throw new BarrierException(makeException(
        null, "Expected a Bool but found " + getType().name));
  }
  public final void call(Context c, String methodName, ArrayList<Value> args) {
    get(c, methodName);
    if (c.exc) return;
    c.value.call(c, args);
  }
  public final void call(Context c, String methodName, Value... args) {
    call(c, methodName, valueArrayToArrayList(args));
  }
  public final Value callOrThrow(String methodName, ArrayList<Value> args) {
    Context c = new Context(null);
    call(c, methodName, args);
    if (c.exc) throw new BarrierException(c.value);
    return c.value;
  }
  public final Value callOrThrow(String methodName, Value... args) {
    return getOrThrow(methodName).callOrThrow(args);
  }
  public final String toString() {
    return callOrThrow("__str__").getStringValue();
  }
  public final boolean isTruthy() {
    return callOrThrow("__bool__").getBoolValue();
  }
  public final String repr() {
    return callOrThrow("__repr__").getStringValue();
  }
  public final int hashCode() {
    return (int) callOrThrow("__hash__").getNumberValue();
  }
  public final boolean equals(Object v) {
    return (v instanceof Value) && callOrThrow("__eq__", (Value) v).isTruthy();
  }
}

public static final class UserValue extends Value {
  public final ClassValue cls;
  public final HashMap<String, Value> attrs;
  public UserValue(ClassValue cls) {
    this(cls, new HashMap<String, Value>());
  }
  public UserValue(ClassValue cls, HashMap<String, Value> attrs) {
    this.cls = cls;
    this.attrs = attrs;
  }
  public ClassValue getType() {
    return cls;
  }
  public Value getOrNull(String name) {
    Value value = super.getOrNull(name);
    return value == null ? attrs.get(name) : value;
  }
  public UserValue put(String name, Value value) {
    attrs.put(name, value);
    return this;
  }
}

public static final class NilValue extends Value {
  public ClassValue getType() {
    return classNil;
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
  public boolean getBoolValue() {
    return value;
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
  public double getNumberValue() {
    return value;
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
  public ArrayList<Value> getListValue()  {
    return value;
  }
}

public static abstract class NamedValue extends Value {
  public final String name;
  public NamedValue(String name) {
    this.name = name == null ? "[anonymous]" : name;
  }
}

public static abstract class FunctionValue extends NamedValue {
  public FunctionValue(String name) {
    super(name);
  }
  public ClassValue getType() {
    return classFunction;
  }
  public abstract void call(Context c, ArrayList<Value> args);
}

public static abstract class BuiltinMethodValue extends FunctionValue {
  public BuiltinMethodValue(String name) {
    super(name);
  }
  public final void call(Context c, ArrayList<Value> args) {
    c.exc = true;
    c.value = makeException(
        c.trace,
        "Can't call a builtin method without binding it first");
  }
  public abstract void callm(Context c, Value owner, ArrayList<Value> args);
  public final Value bind(final Value owner) {
    return new BuiltinFunctionValue(name) {
      public void call(Context c, ArrayList<Value> args) {
        callm(c, owner, args);
      }
    };
  }
}

public static abstract class BuiltinFunctionValue extends FunctionValue {
  public BuiltinFunctionValue(String name) {
    super(name);
  }
}

// TODO: Clean this up.
public static void calls(
    Scope fnScope, String[] fnArgs, String fnVararg, Ast body,
    Context c, Value owner, ArrayList<Value> args) {

  if (fnVararg == null) {
    if (expectArglen(c, fnArgs.length, args)) return;
  } else {
    if (expectArglenStar(c, fnArgs.length, args))
      return;
  }

  Scope scope = new Scope(fnScope);

  if (owner != null)
    scope.put("self", owner);

  for (int i = 0; i < fnArgs.length; i++) {
    String name = fnArgs[i];
    Value value = args.get(i);
    scope.put(name, value);
  }
  if (fnVararg != null) {
    ArrayList<Value> va = new ArrayList<Value>();
    for (int i = fnArgs.length; i < args.size(); i++)
      va.add(args.get(i));
    scope.put(fnVararg, new ListValue(va));
  }

  Scope oldScope = c.scope;
  c.scope = scope;

  body.eval(c);
  if (c.ret)
    c.exc = c.ret = false;

  c.scope = oldScope;
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
  public final void call(Context c, ArrayList<Value> args) {
    calls(scope, this.args, vararg, body, c, null, args);
  }
  public Value bind(final Value owner) {
    return new FunctionValue(name) {
      public final void call(Context c, ArrayList<Value> cargs) {
        calls(scope, args, vararg, body, c, owner, cargs);
      }
    };
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
  public void call(Context c, ArrayList<Value> args) {
    FunctionValue f = (FunctionValue) getForInstance("__new__");
    if (f == null) {
      c.exc = true;
      c.value = makeException(
          c.trace, "Could not create new " + name);
      return;
    }
    f.bind(this).call(c, args);
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

public static TraceValue joinStackTraces(TraceValue left, TraceValue right) {
  if (right == null)
    return left;
  return right == null ? left :
      new TraceValue(right.node, joinStackTraces(left, right.next));
}

// This exception is for crossing barriers where 'Context' was not
// explicitly passed down.
public static class BarrierException extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final Value e;
  public BarrierException(Value e) {
    this.e = e;
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
  public Scope put(NamedValue v) {
    table.put(v.name, v);
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
  public String getLocationString() {
    int a = i, b = i, c = i, lc = 1;
    while (a > 1 && lexer.string.charAt(a-1) != '\n')
      a--;
    while (b < lexer.string.length() && lexer.string.charAt(b) != '\n')
      b++;
    for (int j = 0; j < i; j++)
      if (lexer.string.charAt(j) == '\n')
        lc++;

    String spaces = "";
    for (int j = 0; j < i-a; j++)
      spaces = spaces + " ";

    return
        "*** In file '" + lexer.filespec + "' on line " + Integer.toString(lc) +
        " ***\n" + lexer.string.substring(a, b) + "\n" +
        spaces + "*\n";
  }
}

public static final class TraceValue extends Value {
  public final Ast node;
  public final TraceValue next;
  public TraceValue(Ast node, TraceValue next) {
    this.node = node;
    this.next = next;
  }
  public ClassValue getType() {
    return classTrace;
  }
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

  // For generating error messages.
  public TraceValue trace = null;

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
      c.value = makeException(
          c.trace, "Name '" + name + "' is not defined");
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
    if (c.exc) return;
    Value f = c.value;

    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < this.args.length; i++) {
      this.args[i].eval(c);
      if (c.exc) return;
      args.add(c.value);
    }

    if (this.vararg != null) {
      this.vararg.eval(c);
      if (c.exc) return;

      Value vararg = c.value;
      if (!(vararg instanceof ListValue)) {
        c.exc = true;
        c.value = makeException(
            c.trace,
            "Expected List for vararg but found: " + vararg.getType().name);
        return;
      }

      ArrayList<Value> va = ((ListValue) vararg).value;
      for (int i = 0; i < va.size(); i++)
        args.add(va.get(i));
    }

    TraceValue oldTrace = c.trace;
    try {
      c.trace = new TraceValue(this, oldTrace);
      f.call(c, args);
    } catch (BarrierException e) {
      handleBarrierException(c, e);
    } finally {
      c.trace = oldTrace;
    }
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
    if (c.exc) return;

    Value owner = c.value;

    c.value = owner.getOrNull(attr);

    if (c.value == null) {
      c.exc = true;
      c.value = makeException(
          c.trace, "Couldn't get attribute '" + attr + "'");
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
    if (c.exc) return;
    Value owner = c.value;

    this.val.eval(c);
    if (c.exc) return;
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
      if (c.exc) return;
      bases.add(c.value);
    }

    if (this.varbase != null) {
      this.varbase.eval(c);
      if (c.exc) return;
      Value varbase = c.value;
      if (!(varbase instanceof ListValue)) {
        c.exc = true;
        c.value = makeException(
            c.trace,
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
    if (c.exc) return;
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
    if (c.exc) return;

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
      if (c.exc) return;

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

      if (c.exc) return;
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
    if (c.exc) return;

    if (c.value.isTruthy()) {
      body.eval(c);
    } else if (other != null) {
      other.eval(c);
    } else {
      c.value = nil;
    }
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
      if (c.exc) return;
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
    if (c.exc) return;
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
    if (c.exc) return;

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
    if (c.exc) return;

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
    if (c.exc) return;

    Value left = c.value;

    this.right.eval(c);
    if (c.exc) return;

    Value right = c.value;

    c.value = left == right ? trueValue : falseValue;
  }
}

public static final class ImportAst extends Ast {
  public final String name;
  public ImportAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public void eval(Context c) {
    TraceValue oldTrace = c.trace;
    try {
      c.trace = new TraceValue(this, oldTrace);
      importModule(c, name);
    } catch (BarrierException e) {
      handleBarrierException(c, e);
    } finally {
      c.trace = oldTrace;
    }
    c.scope.put(name, c.value);
  }
}

}
