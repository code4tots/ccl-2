import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

public class Sanity {

/// Effectively the core library.

public static final TypeValue typeValue = new TypeValue("Value");
public static final TypeValue typeType = new TypeValue("Type", typeValue);
public static final TypeValue typeNil = new TypeValue("Nil", typeValue);
public static final TypeValue typeBool = new TypeValue("Bool", typeValue);
public static final TypeValue typeNumber = new TypeValue("Number", typeValue);
public static final TypeValue typeString = new TypeValue("String", typeValue);
public static final TypeValue typeList = new TypeValue("List", typeValue);
public static final TypeValue typeMap = new TypeValue("Map", typeValue);
public static final TypeValue typeFunction = new TypeValue("Function", typeValue)
    .put(new Method("__call__") {
      public final void call(Context c, Value owner, ArrayList<Value> args) {
        ((FunctionValue) owner).call(c, args);
      }
    });

/// Language core dispatch features (i.e. Value and Method)

public static final class Context {

  // Value to propagate, either to the next expression,
  // or as return value, depending on other flags.
  public Value value = null;

  // For the sake of my sanity.
  // Meant to help with debugging.
  public Trace trace = EmptyTrace.instance;

  // Special control flow flags.
  public boolean ret = false; // return
  public boolean br = false; // break
  public boolean cont = false; // continue

  // Verify the snaity of Context
  public final void check() {
    if (value == null)
      err(this, "val is null");
  }

  // Indicates whether any of the special control flow flags are set.
  public final boolean jump() {
    return ret || br || cont;
  }
}

public abstract static class Method {
  public final String name;
  public Method(String name) { this.name = name; }
  public abstract void call(Context c, Value owner, ArrayList<Value> args);
}

public abstract static class Value {
  public abstract TypeValue getType();
  public final void call(Context c, String name, ArrayList<Value> args) {

    TypeValue ownerType = getType();
    TypeValue methodType = null;
    Method method = null;

    for (int i = 0; i < ownerType.mro.size(); i++) {
      methodType = ownerType.mro.get(i);
      method = methodType.methods.get(name);
      if (method != null)
        break;
    }

    if (method == null)
      err(c, "No such method " + name);

    Trace oldTrace = c.trace;
    try {
      c.trace = new CallTrace(c.trace, ownerType, methodType, method);
      method.call(c, this, args);
      c.check();
    } finally {
      c.trace = oldTrace;
    }
  }

  // TODO: Figure out what to do about these java bridge methods.
  // The problem is that I can't pass a 'Context' argument so when there is a
  // problem I can't get a full stack trace.
  public final boolean equals(Object x) { throw new RuntimeException("FUBAR"); }
  public final int hashCode() { throw new RuntimeException("FUBAR"); }
  public final String toString() { throw new RuntimeException("FUBAR"); }
}

public static final class TypeValue extends Value {
  public final String name;
  public final ArrayList<TypeValue> bases = new ArrayList<TypeValue>();
  public final ArrayList<TypeValue> mro = new ArrayList<TypeValue>();
  public final HashMap<String, Method> methods = new HashMap<String, Method>();
  public final TypeValue getType() { return typeType; }
  public TypeValue(String n, ArrayList<Value> bs) {
    if (n == null)
      throw new RuntimeException("FUBAR");

    name = n;
    for (int i = bs.size()-1; i >= 0; i--) {
      TypeValue base = (TypeValue) bs.get(i);
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
  public TypeValue(String n, Value... args) {
    this(n, toArrayList(args));
  }
  public TypeValue put(Method method) {
    methods.put(method.name, method);
    return this;
  }
}

public abstract static class FunctionValue extends Value {
  public final String name;
  public FunctionValue(String name) { this.name = name; }
  public abstract void calli(Context c, ArrayList<Value> args);
  public final void call(Context c, ArrayList<Value> args) {
    calli(c, args);
    c.check();
  }
}

/// Ast

public abstract static class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }
  public abstract void evali(Context c);
  public final void eval(Context c) {
    evali(c);
    c.check();
  }
}

/// Lexer and Token
public static final class Lexer {
  public final String string;
  public final String filespec;
  public final ArrayList<Token> tokens;
  public Lexer(String string, String filespec, int... ii) {
    this.string = string;
    this.filespec = filespec;
    tokens = new ArrayList<Token>();
    for (int i = 0; i < ii.length; i++)
      tokens.add(new Token(this, ii[i]));
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

public static final class CallTrace extends Trace {
  // Type of the object we are calling the method on.
  public final TypeValue valueType;

  // Type of the class that the method is actually implemented in.
  // As such, methodType must be in valueType.mro.
  public final TypeValue methodType;

  // The actual method being invoked.
  // Should be one of the entries in methodType.methods.
  public final Method method;

  public CallTrace(
      Trace next, TypeValue valueType, TypeValue methodType, Method method) {
    super(next);
    this.valueType = valueType;
    this.methodType = methodType;
    this.method = method;
  }

  public final String topToString() {
    return
        "\n*** in method call " + valueType.name + "->" +
        methodType.name + "." + method.name;
  }
}

/// utils
public static ArrayList<Value> toArrayList(Value... args) {
  ArrayList<Value> al = new ArrayList<Value>();
  for (int i = 0; i < args.length; i++)
    al.add(args[i]);
  return al;
}

public static void err(Context c, String message) {
  throw new RuntimeException(message);
}

}
