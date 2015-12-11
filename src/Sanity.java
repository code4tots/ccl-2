import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class Sanity {

// Flag indicating whether we should perform tests on startup.
public static final boolean TEST = true;

// Flag indicating whether test should be verbose
public static final boolean VERBOSE_TEST = true;

public static final HashMap<String, ModuleAst> MODULE_REGISTRY =
    new HashMap<String, ModuleAst>();

// Tests are run in static blocks so we don't need to load them here.
public static void main(String[] args) {
  if (TEST && VERBOSE_TEST)
    System.out.println("/// Builtin set of tests pass! ///");

  if (args.length > 0) {
    ModuleAst mainModule = readModule(args[0]);
    for (int i = 1; i < args.length; i++)
      loadModule(args[i]);

    run(new Context(), mainModule, "__main__");
  }
}

public static ModuleAst readModule(String path) {
  return new Parser(readFile(path), path).parse();
}

public static void loadModule(String path) {
  ModuleAst module = readModule(path);
  MODULE_REGISTRY.put(module.name, module);
}

public static String readFile(String path) {
  try {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line = null;
    StringBuilder sb = new StringBuilder();
    String separator = System.getProperty("line.separator");

    while((line = reader.readLine()) != null) {
      sb.append(line);
      sb.append(separator);
    }

    return sb.toString();
  } catch (IOException e) {
    throw new Fubar("Exception while reading " + path + ": " + e.toString());
  }
}

public static HashMap<String, Value> run(Context c, ModuleAst node, String name) {
  Scope oldScope = c.scope;
  c.scope = new Scope(BUILTIN_SCOPE);
  c.put("__name__", toStringValue(name));
  c.checkStart();
  c.checkEndCall(node.eval(c));
  Scope newScope = c.scope;
  c.scope = oldScope;
  return newScope.table;
}

private static HashMap<String, Value> run(Context c, ModuleAst node) {
  return run(c, node, "<test>");
}

public static Value xeval(String code) {
  return xeval(new Parser(code, "<xeval>").parse());
}

public static Value xeval(Ast node) {
  Context c = new Context();
  c.checkStart();
  return c.checkEndCall(node.eval(c));
}

public static Value importModule(Context c, String name, ModuleAst node) {
  HashMap<String, Value> table = run(c, node, name);
  UserValue value = new UserValue(c, typeModule);

  Iterator<String> it = table.keySet().iterator();
  while (it.hasNext()) {
    String key = it.next();
    value.put(key, table.get(key));
  }

  c.put(name, value);
  return value;
}

public static Value importModule(Context c, String name) {
  ModuleAst node = MODULE_REGISTRY.get(name);
  if (node == null)
    throw err(c, "No module named " + name);
  return importModule(c, name, node);
}

/// Effectively the core library.

public static final NilValue nil = new NilValue();
public static final BoolValue tru = new BoolValue(true);
public static final BoolValue fal = new BoolValue(false);
public static final TypeValue typeValue = new TypeValue(true, "Value", null)
    .put(new Method("__str__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        return owner.call(c, "__repr__", args);
      }
    })
    .put(new Method("__bool__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return tru;
      }
    })
    .put(new Method("__eq__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        return owner == args.get(0) ? tru : fal;
      }
    })
    .put(new Method("__ne__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        return asBoolValue(c, owner.call(c, "__eq__", args), "result of __eq__").value ? fal : tru;
      }
    });
public static final TypeValue typeType = new TypeValue(false, "Type", null, typeValue);
public static final TypeValue typeNil = new TypeValue(false, "Nil", null, typeValue)
    .put(new Method("__repr__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return toStringValue("nil");
      }
    })
    .put(new Method("__bool__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return fal;
      }
    });
public static final TypeValue typeBool = new TypeValue(
    false, "Bool",
    new Constructor() {
      public final Value call(Context c, TypeValue ownerType, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        return asBoolValue(c, args.get(0), "argument 0");
      }
    }, typeValue)
    .put(new Method("__bool__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return owner;
      }
    });
public static final TypeValue typeNumber = new TypeValue(false, "Number", null, typeValue)
    .put(new Method("__repr__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        NumberValue nv = asNumberValue(c, owner, "self");
        double value = nv.value;
        String sv;
        if (value == Math.floor(value))
          sv = Integer.toString((int) value);
        else
          sv = Double.toString(value);
        return toStringValue(sv);
      }
    })
    .put(new Method("__bool__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return asNumberValue(c, owner, "self").value != 0 ? tru : fal;
      }
    })
    .put(new Method("__neg__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return toNumberValue(-asNumberValue(c, owner, "self").value);
      }
    })
    .put(new Method("__eq__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        double left = asNumberValue(c, owner, "self").value;
        double right = asNumberValue(c, args.get(0), "argument 0").value;
        return left == right ? tru : fal;
      }
    })
    .put(new Method("__add__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        if (!(args.get(0) instanceof NumberValue))
          return fal;
        double left = asNumberValue(c, owner, "self").value;
        double right = asNumberValue(c, args.get(0), "argument 0").value;
        return toNumberValue(left + right);
      }
    });
public static final TypeValue typeString = new TypeValue(
    false, "String",
    new Constructor() {
      public final Value call(Context c, TypeValue ownerType, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        return asStringValue(c, args.get(0), "argument 0");
      }
    },
    typeValue)
    .put(new Method("__str__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return owner;
      }
    })
    .put(new Method("__bool__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return asStringValue(c, owner, "self").value.length() != 0 ? tru : fal;
      }
    })
    .put(new Method("__eq__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        if (!(args.get(0) instanceof StringValue))
          return fal;
        String left = asStringValue(c, owner, "self").value;
        String right = asStringValue(c, args.get(0), "argument 0").value;
        return left.equals(right) ? tru : fal;
      }
    });
public static final TypeValue typeList = new TypeValue(
    false, "List",
    new Constructor() {
      public final Value call(Context c, TypeValue ownerType, ArrayList<Value> args) {
        return toListValue(args);
      }
    },
    typeValue)
    .put(new Method("__bool__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return asListValue(c, owner, "self").value.size() != 0 ? tru : fal;
      }
    })
    .put(new Method("__repr__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        StringBuilder sb = new StringBuilder("List[");
        ArrayList<Value> al = asListValue(c, owner, "self").value;
        for (int i = 0; i < al.size(); i++) {
          if (i != 0)
            sb.append(", ");
          sb.append(asStringValue(
              c, al.get(i), "argument " + Integer.toString(i)).value);
        }
        sb.append("]");
        return toStringValue(sb.toString());
      }
    });
public static final TypeValue typeMap = new TypeValue(false, "Map", null, typeValue);
public static final TypeValue typeCallable = new TypeValue(false, "Callable", null, typeValue)
    .put(new Method("__call__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        return asCallableValue(c, owner, "self").call(c, args);
      }
    })
    .put(new Method("__repr__") {
      public final Value call(Context c, Value owner, ArrayList<Value> args) {
        expectArgLen(c, args, 0);
        return toStringValue(asCallableValue(c, owner, "self").name);
      }
    });
public static final TypeValue typeModule = new TypeValue(false, "Module", null, typeValue);

public static final Scope BUILTIN_SCOPE = new Scope(null)
    .put("nil", nil)
    .put("true", tru)
    .put("false", fal)
    .put(typeValue)
    .put(typeNil)
    .put(typeBool)
    .put(typeNumber)
    .put(typeString)
    .put(typeList)
    .put(typeMap)
    .put(typeCallable)
    .put(new FunctionValue("new") {
      public Value calli(Context c, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        return new UserValue(c, args.get(0));
      }
    })
    .put(new FunctionValue("print") {
      public Value calli(Context c, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        StringValue sv = asStringValue(c, args.get(0), "argument 0");
        System.out.println(sv.value);
        return sv;
      }
    })
    .put(new FunctionValue("assert") {
      public Value calli(Context c, ArrayList<Value> args) {
        expectArgLen(c, args, 1);
        if (!asBoolValue(c, args.get(0), "argument 0").value)
          throw err(c, "assertion failed");
        return nil;
      }
    });

public static final class NilValue extends Value {
  public final TypeValue getType() { return typeNil; }
}
public static final class BoolValue extends Value {
  public final Boolean value;
  public BoolValue(Boolean value) { this.value = value; }
  public final TypeValue getType() { return typeBool; }
}
public static final class NumberValue extends Value {
  public final Double value;
  public NumberValue(double value) { this.value = value; }
  public final TypeValue getType() { return typeNumber; }
}
public static final class StringValue extends Value {
  public final String value;
  public StringValue(String value) { this.value = value; }
  public final TypeValue getType() { return typeString; }
}
public static final class ListValue extends Value {
  public final ArrayList<Value> value;
  public ListValue(ArrayList<Value> value) { this.value = value; }
  public final TypeValue getType() { return typeList; }
}

/// Language core dispatch features (i.e. Value and Method)

public static final class Context {

  // For the sake of my sanity.
  // Meant to help with debugging.
  public Trace trace = EmptyTrace.instance;

  // Special control flow flags.
  public boolean return_ = false; // return
  public boolean break_ = false; // break
  public boolean continue_ = false; // continue

  // For variable lookups.
  public Scope scope = new Scope(BUILTIN_SCOPE);

  // Verify the sanity (roll credits!) of Context
  public final void checkStart() {
    if (return_)
      throw err(this, "return_ is set");
    if (break_)
      throw err(this, "break_ is set");
    if (continue_)
      throw err(this, "continue_ is set");
  }
  public final Value checkEndAst(Value result) {
    if (return_ && result == null)
      throw err(this, "return_ flag is set, but there is no return value");
    if (break_ && result != null)
      throw err(this, "break_ flag is set and there is a return value!");
    if (continue_ && result != null)
      throw err(this, "continue_ flag is set and there is a return value!");
    if (!jump() && result == null)
      throw err(this, "None of the jump flags are set, but there is no return value!");
    return result;
  }
  public final Value checkEndCall(Value result) {
    if (return_)
      throw err(this, "return_ is set at the end of a function call");
    if (break_)
      throw err(this, "break_ is set at the end of a function call");
    if (continue_)
      throw err(this, "continue_ is set at the end of a function call");
    if (result == null)
      throw err(this, "There is no resulting value from this call");
    return result;
  }

  // Indicates whether any of the special control flow flags are set.
  public final boolean jump() {
    return return_ || break_ || continue_;
  }

  public final Value get(String name) {
    Value value = scope.getOrNull(name);
    if (value == null)
      throw new Err(trace, "No variable named " + name);
    return value;
  }

  public final Context put(String name, Value value) {
    scope.put(name, value);
    return this;
  }
}

public abstract static class Method {
  public final String name;
  private TypeValue type;
  public Method(String name) { this.name = name; }
  public abstract Value call(Context c, Value owner, ArrayList<Value> args);
  public void setType(TypeValue type) {
    if (this.type != null)
      throw new Fubar("Method " + name + " is already associated with type " + type.name);
    this.type = type;
  }
  public TypeValue getType() {
    if (type == null)
      throw new Fubar("Method " + name + " has not yet been associated with a type yet");
    return type;
  }
}

public abstract static class Constructor {
  private TypeValue type;
  public abstract Value call(Context c, TypeValue actualType, ArrayList<Value> args);
  public Constructor setType(TypeValue type) {
    if (this.type != null)
      throw new Fubar("This constructor is already associated with type " + type.name);
    this.type = type;
    return this;
  }
  public TypeValue getType() {
    if (type == null)
      throw new Fubar("This constructor has not yet been associated with a type yet");
    return type;
  }
}

public abstract static class Value {
  public abstract TypeValue getType();
  public final Value call(Context c, String name, Value... args) {
    return call(c, name, toArrayList(args));
  }
  public final Value call(Context c, String name, ArrayList<Value> args) {
    return getBoundMethod(c, name).call(c, args);
  }

  public BoundMethodValue getBoundMethodOrNull(String name) {
    for (int i = 0; i < getType().mro.size(); i++) {
      TypeValue ancestor = getType().mro.get(i);
      Method method = ancestor.methods.get(name);
      if (method != null)
        return new BoundMethodValue(this, method);
    }
    return null;
  }

  public BoundMethodValue getBoundMethod(Context c, String name) {
    BoundMethodValue method = getBoundMethodOrNull(name);
    if (method == null)
      throw err(c,
        "No method named " + name + " for type " + getType().name);
    return method;
  }

  public final Value get(Context c, String name) {
    if (this instanceof UserValue) {
      Value value = ((UserValue) this).attrs.get(name);
      if (value != null)
        return value;
    }

    BoundMethodValue method = getBoundMethodOrNull(name);
    if (method != null)
      return method;

    throw err(
        c,
        "No method or attribute " + name + " for type " +
        getTypeDescription());
  }

  public final String getTypeDescription() {
    return getType().name + " (" + getClass().getName() + ")";
  }

  // TODO: Figure out what to do about these java bridge methods.
  // The problem is that I can't pass a 'Context' argument so when there is a
  // problem I can't get a full stack trace.
  public final boolean equals(Object x) { throw new Fubar("Value does not support equals"); }
  public final int hashCode() { throw new Fubar("Value does not support hashCode"); }
  public final String toString() { throw new Fubar("Value does not support toString"); }
}

public static final class TypeValue extends Value {
  public final boolean userType;
  public final String name;
  public final ArrayList<TypeValue> bases = new ArrayList<TypeValue>();
  public final ArrayList<TypeValue> mro = new ArrayList<TypeValue>();
  public final HashMap<String, Method> methods = new HashMap<String, Method>();
  public final TypeValue getType() { return typeType; }
  public final Constructor constructor;
  public TypeValue(boolean userType, String n, Constructor cons, ArrayList<Value> bs) {
    if (n == null)
      throw new Fubar("TypeValue needs a name");

    if (cons != null)
      cons.setType(this);

    this.constructor = cons;
    this.userType = userType;

    name = n;
    for (int i = bs.size()-1; i >= 0; i--) {
      TypeValue base = (TypeValue) bs.get(i);
      // TODO: Better error message,
      // TODO: Possibly pass in the Context so that this error can die
      // with a proper stack trace.
      if (userType && !base.userType)
        throw new Fubar(
            "When trying to create type '" + name + "'' tried to use class '" +
            base.name + "' as a base, but user types cannot subclass from " +
            "non-user types.");
      bases.add(base);
      for (int j = base.mro.size()-1; j >=0 ; j--) {
        TypeValue ancestor = base.mro.get(j);
        boolean missing = true;
        for (int k = 0; k < mro.size(); k++) {
          if (ancestor == mro.get(k)) {
            missing = false;
            break;
          }
        }
        if (missing)
          mro.add(ancestor);
      }
    }
    mro.add(this);
    Collections.reverse(mro);
  }
  public TypeValue(boolean userType, String n, Constructor cons, Value... args) {
    this(userType, n, cons, toArrayList(args));
  }
  public TypeValue put(Method method) {
    method.setType(this);
    methods.put(method.name, method);
    return this;
  }
}

public static final class UserValue extends Value {
  public final TypeValue type;
  public final HashMap<String, Value> attrs = new HashMap<String, Value>();
  public UserValue(Context c, Value typeValue) {

    if (!(typeValue instanceof TypeValue))
      throw err(c, "Expected type value but found " + typeValue.getType().name);

    TypeValue type = (TypeValue) typeValue;

    if (!type.userType)
      throw err(c, "Type " + type.name + " is not a user type");

    this.type = type;
  }
  public UserValue put(String name, Value value) {
    attrs.put(name, value);
    return this;
  }
  public final TypeValue getType() { return type; }
}

// Should only be subclassed by BoundMethodValue and FunctionValue.
public abstract static class CallableValue extends Value {
  public final String name;
  public CallableValue(String name) { this.name = name; }
  public final TypeValue getType() { return typeCallable; }
  public abstract Value call(Context c, ArrayList<Value> args);
}

public abstract static class FunctionValue extends CallableValue {
  public FunctionValue(String name) { super(name); }
  public abstract Value calli(Context c, ArrayList<Value> args);
  public final Value call(Context c, ArrayList<Value> args) {
    Trace oldTrace = c.trace;
    try {
      c.trace = new FunctionTrace(c.trace, this);
      c.checkStart();
      return c.checkEndCall(calli(c, args));
    } finally {
      c.trace = oldTrace;
    }
  }
}

public final static class UserFunctionValue extends FunctionValue {
  public final ArrayList<String> args;
  public final String vararg;
  public final Ast body;
  public final Scope scope;
  public UserFunctionValue(
      String name, ArrayList<String> args, String vararg, Ast body,
      Scope scope) {
    super(name);
    this.args = args;
    this.vararg = vararg;
    this.body = body;
    this.scope = scope;
  }
  public final Value calli(Context c, ArrayList<Value> args) {
    if (vararg == null)
      expectArgLen(c, args, this.args.size());
    else
      expectMinArgLen(c, args, this.args.size());

    Scope oldScope = c.scope;
    try {
      c.scope = new Scope(scope);
      for (int i = 0; i < this.args.size(); i++) {
        c.put(this.args.get(i), args.get(i));
      }

      if (vararg != null) {
        ArrayList<Value> va = new ArrayList<Value>();
        for (int i = this.args.size(); i < args.size(); i++) {
          va.add(args.get(i));
        }
        c.put(vararg, toListValue(va));
      }

      Value result = body.eval(c);

      if (!c.return_)
        result = nil;

      c.return_ = false;

      c.checkStart();

      return result;

    } finally {
      c.scope = oldScope;
    }
  }
}

public static final class BoundMethodValue extends CallableValue {
  public final Value owner;
  public final Method method;
  public BoundMethodValue(Value owner, Method method) {
    super(method.name);
    this.owner = owner;
    this.method = method;
  }
  public final Value call(Context c, ArrayList<Value> args) {
    Trace oldTrace = c.trace;
    try {
      c.trace =
          new MethodTrace(c.trace, owner.getType(), method.getType(), method);
      c.checkStart();
      return c.checkEndCall(method.call(c, owner, args));
    } finally {
      c.trace = oldTrace;
    }
  }
}

/// Trace

public abstract static class Trace {
  public final Trace next;
  public Trace(Trace next) { this.next = next; }
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    for (Trace t = this; t != null; t = t.next) {
      sb.append(t.topToString());
    }
    return sb.toString();
  }
  public abstract String topToString();
}

public static final class EmptyTrace extends Trace {
  private EmptyTrace() { super(null); }
  public static final EmptyTrace instance = new EmptyTrace();
  public final String topToString() { return ""; }
}

public static final class AstTrace extends Trace {
  public final Ast node;
  public AstTrace(Trace next, Ast node) { super(next); this.node = node; }
  public final String topToString() {
    return "\n" + node.token.getLocationString(); // TODO
  }
}

public static final class MethodTrace extends Trace {
  // Type of the object we are calling the method on.
  public final TypeValue valueType;

  // Type of the class that the method is actually implemented in.
  // As such, methodType must be in valueType.mro.
  public final TypeValue methodType;

  // The actual method being invoked.
  // Should be one of the entries in methodType.methods.
  public final Method method;

  public MethodTrace(
      Trace next, TypeValue valueType, TypeValue methodType, Method method) {
    super(next);
    this.valueType = valueType;
    this.methodType = methodType;
    this.method = method;
  }

  public final String topToString() {
    return
        "\n*** in method " + valueType.name + "->" +
        methodType.name + "." + method.name;
  }
}

public static final class FunctionTrace extends Trace {
  public final FunctionValue f;

  public FunctionTrace(Trace next, FunctionValue f) {
    super(next);
    this.f = f;
  }

  public final String topToString() {
    return
        "\n*** in function " + f.name;
  }
}

/// Ast

public abstract static class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }
  public abstract Value evali(Context c);
  public final Value eval(Context c) {
    c.checkStart();
    return c.checkEndAst(evali(c));
  }
}

public static final class NameAst extends Ast {
  public final String name;
  public NameAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public final Value evali(Context c) {
    Trace oldTrace = c.trace;
    try {
      c.trace = new AstTrace(c.trace, this);
      return c.get(name);
    } finally {
      c.trace = oldTrace;
    }
  }
}

public static final class StringAst extends Ast {
  public final StringValue value;
  public StringAst(Token token, String value) {
    super(token);
    this.value = toStringValue(value);
  }
  public final Value evali(Context c) {
    return value;
  }
}

public static final class NumberAst extends Ast {
  public final NumberValue value;
  public NumberAst(Token token, Double value) {
    super(token);
    this.value = toNumberValue(value);
  }
  public final Value evali(Context c) {
    return value;
  }
}

public static final class AssignAst extends Ast {
  public final String name;
  public final Ast value;
  public AssignAst(Token token, String name, Ast value) {
    super(token);
    this.name = name;
    this.value = value;
  }
  public final Value evali(Context c) {
    Value value = this.value.eval(c);
    if (c.jump())
      return value;

    Trace oldTrace = c.trace;
    try {
      c.trace = new AstTrace(c.trace, this);
      c.put(name, value);
      return value;
    } finally {
      c.trace = oldTrace;
    }
  }
}

public static final class FunctionAst extends Ast {
  public final String name;
  public final ArrayList<String> args;
  public final String vararg;
  public final Ast body;
  public FunctionAst(
      Token token, String name, ArrayList<String> args, String vararg,
      Ast body) {
    super(token);
    this.name = name;
    this.args = args;
    this.vararg = vararg;
    this.body = body;
  }
  public final Value evali(Context c) {
    FunctionValue f = new UserFunctionValue(name, args, vararg, body, c.scope);
    c.put(name, f);
    return f;
  }
}

public static final class ReturnAst extends Ast {
  public final Ast value;
  public ReturnAst(Token token, Ast value) {
    super(token);
    this.value = value;
  }
  public final Value evali(Context c) {
    Value result = value.eval(c);
    if (c.jump())
      return result;

    c.return_ = true;
    return result;
  }
}

public static final class CallAst extends Ast {
  public final Ast owner;
  public final ArrayList<Ast> args;
  public final Ast vararg;
  public CallAst(Token token, Ast owner, ArrayList<Ast> args, Ast vararg) {
    super(token);
    this.owner = owner;
    this.args = args;
    this.vararg = vararg;
  }
  public final Value evali(Context c) {

    Value owner = this.owner.eval(c);
    if (c.jump())
      return owner;

    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < this.args.size(); i++) {

      Value arg = this.args.get(i).eval(c);
      if (c.jump())
        return arg;

      args.add(arg);
    }

    if (this.vararg != null) {
      Value value = this.vararg.eval(c);
      if (c.jump())
        return value;

      if (!(value instanceof ListValue))
        throw err(
            c,
            "Splat argument must evaluate to a List but found " +
            value.getTypeDescription());

      ArrayList<Value> va = ((ListValue) value).value;

      for (int i = 0; i < va.size(); i++)
        args.add(va.get(i));
    }

    Trace oldTrace = c.trace;
    try {
      c.trace = new AstTrace(c.trace, this);
      return invoke(c, owner, args);
    } finally {
      c.trace = oldTrace;
    }
  }
}

public static final class IsAst extends Ast {
  public final Ast left, right;
  public IsAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Value evali(Context c) {
    return left.eval(c) == right.eval(c) ? tru : fal;
  }
}

public static final class OperationAst extends Ast {
  public final Ast owner;
  public final String name;
  public final ArrayList<Ast> args;
  public OperationAst(Token token, Ast owner, String name, Ast... args) {
    super(token);
    this.owner = owner;
    this.name = name;
    this.args = toArrayList(args);
  }
  public final Value evali(Context c) {
    Value owner = this.owner.eval(c);
    if (c.jump())
      return owner;

    ArrayList<Value> args = new ArrayList<Value>();
    for (int i = 0; i < this.args.size(); i++) {

      Value arg = this.args.get(i).eval(c);
      if (c.jump())
        return arg;

      args.add(arg);
    }

    Trace oldTrace = c.trace;
    try {
      c.trace = new AstTrace(c.trace, this);
      return owner.call(c, name, args);
    } finally {
      c.trace = oldTrace;
    }
  }
}

public static final class NotAst extends Ast {
  public final Ast target;
  public NotAst(Token token, Ast target) {
    super(token);
    this.target = target;
  }
  public final Value evali(Context c) {
    Value target = this.target.eval(c);
    if (c.jump())
      return target;

    return asBoolValue(c, target, "argument to 'not'").value ? fal : tru;
  }
}

public static final class AndAst extends Ast {
  public final Ast left, right;
  public AndAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Value evali(Context c) {
    Value left = this.left.eval(c);
    if (c.jump())
      return left;

    if (!asBoolValue(c, left, "left argument to 'and'").value)
      return left;

    return this.right.eval(c);
  }
}

public static final class OrAst extends Ast {
  public final Ast left, right;
  public OrAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Value evali(Context c) {
    Value left = this.left.eval(c);
    if (c.jump())
      return left;

    if (asBoolValue(c, left, "left argument to 'or'").value)
      return left;

    return this.right.eval(c);
  }
}

public static final class SetItemAst extends Ast {
  public final Ast owner;
  public final Ast index;
  public final Ast value;
  public SetItemAst(Token token, Ast owner, Ast index, Ast value) {
    super(token);
    this.owner = owner;
    this.index = index;
    this.value = value;
  }
  public final Value evali(Context c) {

    Value owner = this.owner.eval(c);
    if (c.jump())
      return owner;

    Value index = this.index.eval(c);
    if (c.jump())
      return index;

    Value value = this.value.eval(c);
    if (c.jump())
      return value;

    return owner.call(c, "__setitem__", index, value);
  }
}

public static final class GetAttributeAst extends Ast {
  public final Ast owner;
  public final String attribute;
  public GetAttributeAst(Token token, Ast owner, String attribute) {
    super(token);
    this.owner = owner;
    this.attribute = attribute;
  }
  public final Value evali(Context c) {

    Value value = owner.eval(c);
    if (c.jump())
      return value;

    return value.get(c, attribute);
  }
}
public static final class SetAttributeAst extends Ast {
  public final Ast owner;
  public final String attribute;
  public final Ast value;
  public SetAttributeAst(Token token, Ast owner, String attribute, Ast value) {
    super(token);
    this.owner = owner;
    this.attribute = attribute;
    this.value = value;
  }
  public final Value evali(Context c) {

    Value ownerValue = this.owner.eval(c);
    if (c.jump())
      return ownerValue;

    if (!(ownerValue instanceof UserValue))
      throw err(
          c,
          "Values of type " + ownerValue.getTypeDescription() +
          " cannot have its attributes set");

    UserValue owner = (UserValue) ownerValue;
    Value value = this.value.eval(c);
    owner.put(attribute, value);
    return value;
  }
}

public static final class BlockAst extends Ast {
  public final ArrayList<Ast> body;
  public BlockAst(Token token, ArrayList<Ast> body) {
    super(token);
    this.body = body;
  }
  public final Value evali(Context c) {
    Value last = nil;
    for (int i = 0; i < body.size(); i++) {
      last = body.get(i).eval(c);
      if (c.jump())
        return last;
    }
    return last;
  }
}

public static final class ModuleAst extends Ast {
  public final String name;
  public final BlockAst body;
  public ModuleAst(Token token, String name, BlockAst body) {
    super(token);
    this.name = name;
    this.body = body;
  }
  public final Value evali(Context c) {
    return body.eval(c);
  }
}

// Ast test

static {
  if (TEST) {
    Context c = new Context();
    Value value;

    value = xeval("5");
    expect(value instanceof NumberValue);
    expect(((NumberValue) value).value.equals(5.0));

    value = xeval("nil");
    expect(value == nil);

    value = xeval("nil.__str__[]");
    expect(value instanceof StringValue);
    expect(((StringValue) value).value.equals("nil"));

    value = xeval("new[Value]");
    expect(value instanceof UserValue);
    expect(((UserValue) value).type == typeValue);

    value = xeval("a = 5 a");
    expect(value instanceof NumberValue);
    expect(((NumberValue) value).value.equals(5.0));

    value = xeval("x = new[Value] x.a = 6.2 x.a");
    expect(value instanceof NumberValue);
    expect(((NumberValue) value).value.equals(6.2));

    // System.out.println("Ast tests pass");
  }
}

/// Scope

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
  public Scope put(TypeValue t) {
    table.put(t.name, t);
    return this;
  }
}

/// Parser

public static final class Parser {
  public final Lexer lexer;
  public final String name;
  public final String filespec;
  public Parser(String string, String filespec) {
    this(new Lexer(string, filespec));
  }
  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.filespec = lexer.filespec;
    this.name = filespecToName(lexer.filespec);
  }
  private Token peek() { return lexer.peek; }
  private Token next() { return lexer.next(); }
  private boolean at(String type) { return peek().type.equals(type); }
  private boolean consume(String type) {
    if (at(type)) {
      next();
      return true;
    }
    return false;
  }
  private Token expect(String type) {
    if (!at(type))
      throw new SyntaxError(
          peek(), "Expected " + type + " but found " + peek().type);
    return next();
  }
  public ModuleAst parse() {
    Token token = peek();
    ArrayList<Ast> exprs = new ArrayList<Ast>();
    while (!at("EOF"))
      exprs.add(parseExpression());
    return new ModuleAst(token, name, new BlockAst(token, exprs));
  }
  public Ast parseExpression() {
    return parseOrExpression();
  }
  public Ast parseOrExpression() {
    Ast node = parseAndExpression();
    while (true) {
      if (at("or")) {
        Token token = next();
        Ast right = parseAndExpression();
        node = new OrAst(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseAndExpression() {
    Ast node = parseCompareExpression();
    while (true) {
      if (at("and")) {
        Token token = next();
        Ast right = parseCompareExpression();
        node = new AndAst(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseCompareExpression() {
    Ast node = parseAdditiveExpression();
    while (true) {
      if (at("==")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new OperationAst(token, node, "__eq__", right);
        continue;
      }
      if (at("!=")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new OperationAst(token, node, "__ne__", right);
        continue;
      }
      if (at("is")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new IsAst(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseAdditiveExpression() {
    Ast node = parseMultiplicativeExpression();
    while (true) {
      if (at("+")) {
        Token token = next();
        Ast right = parseMultiplicativeExpression();
        node = new OperationAst(token, node, "__add__", right);
        continue;
      }
      if (at("-")) {
        Token token = next();
        Ast right = parseMultiplicativeExpression();
        node = new OperationAst(token, node, "__sub__", right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseMultiplicativeExpression() {
    Ast node = parsePrefixExpression();
    while (true) {
      if (at("*")) {
        Token token = next();
        Ast right = parsePrefixExpression();
        node = new OperationAst(token, node, "__mul__", right);
        continue;
      }
      if (at("/")) {
        Token token = next();
        Ast right = parsePrefixExpression();
        node = new OperationAst(token, node, "__div__", right);
        continue;
      }
      if (at("%")) {
        Token token = next();
        Ast right = parsePrefixExpression();
        node = new OperationAst(token, node, "__mod__", right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parsePrefixExpression() {
    if (at("+")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      return new OperationAst(token, node, "__pos__");
    }
    if (at("-")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      return new OperationAst(token, node, "__neg__");
    }
    if (at("not")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      return new NotAst(token, node);
    }
    return parsePostfixExpression();
  }
  public Ast parsePostfixExpression() {
    Ast node = parsePrimaryExpression();
    while (true) {
      if (at("[")) {
        Token token = next();
        ArrayList<Ast> args = new ArrayList<Ast>();
        while (!at("]") && !at("*")) {
          args.add(parseExpression());
          consume(",");
        }
        Ast vararg = null;
        if (consume("*"))
          vararg = parseExpression();
        expect("]");
        if (at("=")) {
          token = next();
          if (vararg != null || args.size() != 1)
            throw new SyntaxError(
                token, "For setitem syntax, must have exactly one argument");
          node = new SetItemAst(token, node, args.get(0), parseExpression());
        } else {
          node = new CallAst(token, node, args, vararg);
        }
        continue;
      }

      if (at(".")) {
        Token token = next();
        String name = (String) expect("ID").value;

        if (at("=")) {
          token = next();
          Ast value = parseExpression();
          node = new SetAttributeAst(token, node, name, value);
        } else {
          node = new GetAttributeAst(token, node, name);
        }
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parsePrimaryExpression() {

    if (at("STR")) {
      Token token = next();
      return new StringAst(token, (String) token.value);
    }

    if (at("NUM")) {
      Token token = next();
      return new NumberAst(token, (Double) token.value);
    }

    if (at("ID")) {
      Token token = next();
      String name = (String) token.value;

      if (at("=")) {
        token = next();
        Ast value = parseExpression();
        return new AssignAst(token, name, value);
      } else {
        return new NameAst(token, name);
      }
    }

    if (consume("(")) {
      Ast expr = parseExpression();
      expect(")");
      return expr;
    }

    if (at("{")) {
      Token token = next();
      ArrayList<Ast> exprs = new ArrayList<Ast>();
      while (!at("}"))
        exprs.add(parseExpression());
      expect("}");
      return new BlockAst(token, exprs);
    }

    if (at("return")) {
      Token token = next();
      Ast value = parseExpression();
      return new ReturnAst(token, value);
    }

    if (at("def")) {
      Token token = next();
      String name = (String) expect("ID").value;
      expect("[");
      ArrayList<String> args = new ArrayList<String>();
      while (at("ID")) {
        args.add((String) expect("ID").value);
        consume(",");
      }
      String vararg = null;
      if (consume("*")) {
        vararg = (String) expect("ID").value;
      }
      expect("]");
      Ast body = parseExpression();
      return new FunctionAst(token, name, args, vararg, body);
    }

    throw new SyntaxError(peek(), "Expected expression");
  }
}

// Parser test
static {
  if (TEST) {
    Parser parser;
    Ast node;

    parser = new Parser("5", "<test>");
    node = parser.parseExpression();
    expect(node instanceof NumberAst);
    expect(((NumberAst) node).value.value.equals(5.0));

    parser = new Parser("'hi'", "<test>");
    node = parser.parseExpression();
    expect(node instanceof StringAst);
    expect(((StringAst) node).value.value.equals("hi"));

    parser = new Parser("hi", "<test>");
    node = parser.parseExpression();
    expect(node instanceof NameAst);
    expect(((NameAst) node).name.equals("hi"));

    parser = new Parser("(hi)", "<test>");
    node = parser.parseExpression();
    expect(node instanceof NameAst);
    expect(((NameAst) node).name.equals("hi"));

    // System.out.println("Parser tests pass");
  }
}

/// Lexer and Token

public static final class Lexer {
  public static final ArrayList<String> KEYWORDS = toArrayList(
      "and", "or", "xor", "return", "is",
      "def", "not");
  public static final ArrayList<String> SYMBOLS;

  // My syntax highlighter does funny things if it sees "{", "}" in the
  // surrounding scope.
  static {
    SYMBOLS = toArrayList(
        "(", ")", "[", "]", "{", "}", ".", ":", ",",
        "=", "==", "!=", "<", "<=", ">", ">=",
        "+", "-", "*", "/", "%");
  }

  public final String string;
  public final String filespec;
  public final ArrayList<Token> tokens;
  public boolean done = false;
  public Token peek = null;
  public int pos = 0;
  public Lexer(String string, String filespec) {
    this.string = string;
    this.filespec = filespec;
    tokens = new ArrayList<Token>();
    peek = null;

    next();
  }
  public Token next() {
    Token token = peek;
    peek = extract();
    tokens.add(peek);
    return token;
  }
  private Token extract() {
    skipSpacesAndComments();

    if (!more()) {
      done = true;
      return makeToken(pos, "EOF");
    }

    int start = pos;

    // STR
    if (startsWith("r'", "'", "r\"", "\"")) {
      boolean raw = false;
      if (startsWith("r")) {
        pos++;
        raw = true;
      }
      String quote = startsWith("'''", "\"\"\"") ? slice(3) : slice(1);
      pos += quote.length();
      StringBuilder sb = new StringBuilder();
      while (!startsWith(quote)) {
        if (!more())
          throw new SyntaxError(
              makeToken(start, "ERR"),
              "Finish your string literals");
        if (!raw && startsWith("\\")) {
          pos++;
          if (!more())
            throw new SyntaxError(
                makeToken(pos, "ERR"),
                "Finish your string escapes");
          switch (ch()) {
          case '\\': sb.append('\\'); break;
          default: throw new SyntaxError(
              makeToken(pos, "ERR"), 
              "Invalid string escape " + Character.toString(ch()));
          }
          pos++;
        } else {
          sb.append(ch());
          pos++;
        }
      }
      pos += quote.length();
      return makeToken(start, "STR", sb.toString());
    }

    // NUM
    boolean seenDot = false;
    if (startsWith("+", "-"))
      pos++;
    if (startsWith(".")) {
      seenDot = true;
      pos++;
    }
    if (isdigit()) {
      while (isdigit())
        pos++;
      if (!seenDot && startsWith(".")) {
        pos++;
        while (isdigit())
          pos++;
      }
      return makeToken(start, "NUM", Double.valueOf(cut(start)));
    } else {
      pos = start;
    }

    // ID and KEYWORDS
    while (isword())
      pos++;
    if (start < pos) {
      String word = cut(start);
      if (KEYWORDS.contains(word))
        return makeToken(start, word);
      else
        return makeToken(start, "ID", word);
    }

    // SYMBOLS
    String symbol = null;
    for (int i = 0; i < SYMBOLS.size(); i++) {
      String s = SYMBOLS.get(i);
      if (startsWith(s) && (symbol == null || s.length() > symbol.length()))
        symbol = s;
    }
    if (symbol != null) {
      pos += symbol.length();
      return makeToken(start, symbol);
    }

    // ERR
    while (more() && !Character.isWhitespace(ch()))
      pos++;
    Token token = makeToken(start, "ERR");
    throw new SyntaxError(token, "Unrecognized token");
  }
  public boolean isdigit() {
    return more() && Character.isDigit(ch());
  }
  public boolean isword() {
    return more() && (
        Character.isAlphabetic(ch()) || ch() == '_' || isdigit());
  }
  private boolean more() {
    return pos < string.length();
  }
  private boolean isspace() {
    return more() && Character.isWhitespace(ch());
  }
  private String slice(int len) {
    int end = pos + len < string.length() ? pos + len : string.length();
    return string.substring(pos, end);
  }
  private String cut(int start) {
    return string.substring(start, pos);
  }
  private char ch() {
    return string.charAt(pos);
  }
  private void skipSpacesAndComments() {
    while (true) {
      while (isspace())
        pos++;

      if (!startsWith("#"))
        break;

      while (more() && !startsWith("\n"))
        pos++;
    }
  }
  private boolean startsWith(String... args) {
    for (int i = 0; i < args.length; i++)
      if (string.startsWith(args[i], pos))
        return true;
    return false;
  }
  private Token makeToken(int start, String type) {
    return makeToken(start, type, null);
  }
  private Token makeToken(int start, String type, Object value) {
    return new Token(this, start, type, value);
  }
}

public static final class Token {
  public final Lexer lexer;
  public final int i;
  public final String type;
  public final Object value;
  public Token(Lexer lexer, int i, String type, Object value) {
    this.lexer = lexer;
    this.i = i;
    this.type = type;
    this.value = value;
  }
  public String getLocationString() {
    int a = i, b = i, c = i, lc = 1;
    while (a > 0 && lexer.string.charAt(a-1) != '\n')
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
        "*** in file '" + lexer.filespec + "' on line " + Integer.toString(lc) +
        " ***\n" + lexer.string.substring(a, b) + "\n" +
        spaces + "*\n";
  }
}

// Lexer test
static {
  if (TEST) {
    Lexer lexer;
    Token tok;

    lexer = new Lexer("'hi'", "<test>");
    tok = lexer.next();
    expect(tok.type.equals("STR"));
    expect(tok.value.equals("hi"));

    lexer = new Lexer("5.6", "<test>");
    tok = lexer.next();
    expect(tok.type.equals("NUM"));
    expect(tok.value.equals(5.6));

    lexer = new Lexer("hi", "<test>");
    tok = lexer.next();
    expect(tok.type.equals("ID"));
    expect(tok.value.equals("hi"));

    lexer = new Lexer("def", "<test>");
    tok = lexer.next();
    expect(tok.type.equals("def"));
    expect(tok.value == null);

    lexer = new Lexer(".", "<test>");
    tok = lexer.next();
    expect(tok.type.equals("."));
    expect(tok.value == null);

    lexer = new Lexer("a # b\nc", "<test>");
    while (!lexer.done)
      lexer.next();

    expect(lexer.tokens.size() == 3);
    expect(lexer.tokens.get(0).type.equals("ID"));
    expect(lexer.tokens.get(1).type.equals("ID"));
    expect(lexer.tokens.get(2).type.equals("EOF"));

    // System.out.println("Lexer tests pass");
  }
}

/// Exceptions

// SyntaxError is thrown if an error is encountered in either
// the lex or parse process.
public static final class SyntaxError extends RuntimeException {
  public final static long serialVersionUID = 42L;
  public final Token token;
  public final String message;
  public SyntaxError(Token token, String message) {
    super(message + "\n" + token.getLocationString());
    this.token = token;
    this.message = message;
  }
}

// Fubar exception is thrown whenever we have an error but no context
// instance to get a stack trace.
public static final class Fubar extends RuntimeException {
  public final static long serialVersionUID = 42L;
  public Fubar(String message) { super(message); }
}

// Err is the general exception thrown whenever we encounter an error
// while a fully parsed program is running.
public static final class Err extends RuntimeException {
  public final static long serialVersionUID = 42L;
  public final Trace trace;
  public final String message;
  public Err(Trace trace, String message) {
    super(message + "\n" + trace.toString());
    this.trace = trace;
    this.message = message;
  }
}

/// utils
public static ArrayList<Value> toArrayList(Value... args) {
  ArrayList<Value> al = new ArrayList<Value>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}
public static ArrayList<Ast> toArrayList(Ast... args) {
  ArrayList<Ast> al = new ArrayList<Ast>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}
public static ArrayList<String> toArrayList(String... args) {
  ArrayList<String> al = new ArrayList<String>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}

public static Err err(Context c, String message) {
  return new Err(c.trace, message);
}

public static void expect(boolean cond) {
  if (!cond) throw new Fubar("Assertion failed");
}

public static String filespecToName(String filespec) {
  int start, end = filespec.length();
  for (start = filespec.length()-1;
        start >= 1 && filespec.charAt(start-1) != '/' &&
        filespec.charAt(start-1) != '\\'; start--);
  if (filespec.endsWith(".ccl"))
    end -= ".ccl".length();
  return filespec.substring(start, end);
}

public static void expectArgLen(Context c, ArrayList<Value> args, int len) {
  if (args.size() != len)
    throw err(
        c,
        "Expected argument of length " + Integer.toString(len) +
        " but found " + Integer.toString(args.size()) + " arguments.");
}

public static void expectMinArgLen(Context c, ArrayList<Value> args, int len) {
  if (args.size() >= len)
    throw err(
        c,
        "Expected at least " + Integer.toString(len) + " arguments" +
        " but found " + Integer.toString(args.size()) + " arguments.");
}

public static Value invoke(Context c, Value owner, ArrayList<Value> args) {
  // Technically this branching is unnecessary --
  // It's just that this gives nicer looking stack trace.
  if (owner instanceof CallableValue)
    return ((CallableValue) owner).call(c, args);
  else if (owner instanceof TypeValue) {
    Constructor constructor = ((TypeValue) owner).constructor;
    if (constructor == null)
      throw err(c, "Type " + ((TypeValue) owner).name + " has no constructor");
    return constructor.call(c, (TypeValue) owner, args);
  }
  else
    return owner.call(c, "__call__", args);
}

/// conversion/extraction to Java type utils

public static StringValue asStringValue(Context c, Value value, String name) {
  if (value instanceof StringValue)
    return (StringValue) value;

  Value result = value.call(c, "__str__");

  if (!(result instanceof StringValue))
    throw err(
        c,
        "Expected the result of __str__ on " + name +
        " to be a String but found " +
        result.getTypeDescription());

  return (StringValue) result;
}

public static BoolValue asBoolValue(Context c, Value value, String name) {
  if (value instanceof BoolValue)
    return (BoolValue) value;

  Value result = value.call(c, "__bool__");

  if (!(result instanceof BoolValue))
    throw err(
        c,
        "Expected the result of __bool__ on " + name +
        " to be a Bool but found " +
        result.getTypeDescription());

  return (BoolValue) result;
}

public static NumberValue asNumberValue(Context c, Value value, String name) {
  if (!(value instanceof NumberValue))
    throw err(
        c,
        "Expected " + name + " to be a Number but found " +
        value.getTypeDescription());

  return (NumberValue) value;
}

public static ListValue asListValue(Context c, Value value, String name) {
  if (!(value instanceof ListValue))
    throw err(
        c,
        "Expected " + name + " to be a List but found " +
        value.getTypeDescription());

  return (ListValue) value;
}

public static CallableValue asCallableValue(Context c, Value value, String name) {
  if (!(value instanceof CallableValue))
    throw err(
        c,
        "Expected " + name + " to be a Callable but found " +
        value.getTypeDescription());

  return (CallableValue) value;
}

public static NumberValue toNumberValue(double d) {
  return new NumberValue(d);
}

public static StringValue toStringValue(String s) {
  return new StringValue(s);
}

public static ListValue toListValue(ArrayList<Value> a) {
  return new ListValue(a);
}

}
