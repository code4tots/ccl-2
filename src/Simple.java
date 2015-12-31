import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class Simple {

//// Language core library.
// The stuff that gets included before anything is imported.

public final BuiltinFunc native_eq = new BuiltinFunc("__eq__") {
  public Val calli(Val self, ArrayList<Val> args) {
    expectExactArgumentLength(args, 1);
    return self.equals(args.get(0)) ? tru : fal;
  }
};
public final BuiltinFunc native_repr = new BuiltinFunc("__repr__") {
  public Val calli(Val self, ArrayList<Val> args) {
    expectExactArgumentLength(args, 0);
    return toStr(self.repr());
  }
};

// Meta blobs for builtin types.
public final Nil nil = new Nil();
public final Bool tru = new Bool(true);
public final Bool fal = new Bool(false);
public final HashMap<String, Val> ROOT_META_BLOB =
    new BlobHashMapFactory()
    .put("__name__", toStr("MetaBlob"))
    .put(new BuiltinFunc("__add__") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        HashMap<String, Val> attrs = new HashMap<String, Val>();
        attrs.putAll(asBlob(self, "self").attrs);
        attrs.putAll(asBlob(args.get(0), "argument 0").attrs);
        return new Blob(ROOT_META_BLOB, attrs);
      }
    })
    .put(new BuiltinFunc("extend") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        HashMap<String, Val> attrs = asBlob(self, "self").attrs;
        Iterator<HashMap.Entry<String, Val>> iter =
            asBlob(args.get(0), "argument 0").attrs.entrySet().iterator();
        while (iter.hasNext()) {
          HashMap.Entry<String, Val> ent = iter.next();
          if (attrs.get(ent.getKey()) == null)
            attrs.put(ent.getKey(), ent.getValue());
        }
        return self;
      }
    })
    .put(new BuiltinFunc("__repr__") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 0);
        Val v = asBlob(self, "self").get("__name__");
        return v == null ?
            toStr("<unnamed meta blob>") : asStr(v, "__name__");
      }
    })
    .get();
public final Blob MB_META_BLOB = new Blob(ROOT_META_BLOB, ROOT_META_BLOB);
public final Blob MB_VAL = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Val"));
public final Blob MB_NIL = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Nil"))
    .put(native_eq).put(native_repr);
public final Blob MB_BOOL = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Bool"))
    .put(native_eq).put(native_repr);
public final Blob MB_NUM = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Num"))
    .put(native_eq).put(native_repr)
    .put(new BuiltinFunc("__lt__") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        Num left = asNum(self, "self");
        Num right = asNum(args.get(0), "argument 0");
        return left.getVal() < right.getVal() ? tru : fal;
      }
    })
    .put(new BuiltinFunc("__add__") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        Num left = asNum(self, "self");
        Num right = asNum(args.get(0), "argument 0");
        return toNum(left.getVal() + right.getVal());
      }
    })
    .put(new BuiltinFunc("__sub__") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        Num left = asNum(self, "self");
        Num right = asNum(args.get(0), "argument 0");
        return toNum(left.getVal() - right.getVal());
      }
    });
public final Blob MB_STR = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Str"))
    .put(native_eq).put(native_repr)
    .put(new BuiltinFunc("__add__") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        Str left = asStr(self, "self");
        Str right = asStr(args.get(0), "argument 0");
        return toStr(left.getVal() + right.getVal());
      }
    });
public final Blob MB_LIST = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("List"))
    .put(native_eq).put(native_repr)
    .put(new BuiltinFunc("len") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 0);
        return toNum(asList(self, "self").getVal().size());
      }
    })
    .put(new BuiltinFunc("add") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        asList(self, "self").getVal().add(args.get(0));
        return self;
      }
    });
public final Blob MB_MAP = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Map"))
    .put(native_eq).put(native_repr);
public final Blob MB_FUNC = new Blob(ROOT_META_BLOB)
    .put("__name__", toStr("Func"))
    .put(native_eq).put(native_repr)
    .put(new BuiltinFunc("apply") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 2);
        return asFunc(self, "self").call(
            args.get(0),
            asList(args.get(1), "argument 1").getVal());
      }
    });

// Builtins
public final Scope GLOBALS = new Scope(null)
    .put("nil", nil)
    .put("true", tru)
    .put("false", fal)
    .put("MetaBlob", MB_META_BLOB)
    .put("Val", MB_VAL)
    .put("Nil", MB_NIL)
    .put("Bool", MB_BOOL)
    .put("Num", MB_NUM)
    .put("Str", MB_STR)
    .put("List", MB_LIST)
    .put("Map", MB_MAP)
    .put("Func", MB_FUNC)
    .put(new BuiltinFunc("print") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        System.out.println(args.get(0));
        return args.get(0);
      }
    })
    .put(new BuiltinFunc("assert") {
      public Val calli(Val self, ArrayList<Val> args) {
        if (args.size() != 1 && args.size() != 2)
          throw err("Expected 1 or 2 arguments but found " + args.size());

        if (!args.get(0).truthy()) {
          String message = "Assertion error";
          if (args.size() == 2)
            message += ": " + args.get(1).toString();
          throw err(message);
        }
        return nil;
      }
    })
    .put(new BuiltinFunc("err") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        throw err(args.get(0).toString());
      }
    })
    .put(new BuiltinFunc("new") {
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        return new Blob(asBlob(args.get(0), "argument 0"));
      }
    })
    .put(new BuiltinFunc("C") { // 'C' is for 'class'
      public Val calli(Val self, ArrayList<Val> args) {
        if (args.size() % 2 != 0)
          throw err("'C' requires an even number of arguments");
        HashMap<String, Val> attrs = new HashMap<String, Val>();
        for (int i = 0; i < args.size(); i += 2)
          attrs.put(asStr(args.get(i), i).getVal(), args.get(i+1));
        return new Blob(ROOT_META_BLOB, attrs);
      }
    })
    .put(new BuiltinFunc("L") { // 'L' is for 'list'
      public Val calli(Val self, ArrayList<Val> args) {
        return new List(args);
      }
    })
    .put(new BuiltinFunc("M") { // 'M' is for 'map'
      public Val calli(Val self, ArrayList<Val> args) {
        if (args.size() % 2 != 0)
          throw err("'M' requires an even number of arguments");
        HashMap<Val, Val> map = new HashMap<Val, Val>();
        for (int i = 0; i < args.size(); i += 2)
          map.put(args.get(i), args.get(i+1));
        return new Map(map);
      }
    })
    .put(new BuiltinFunc("S") { // 'S' is for 'str'
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        return toStr(args.get(0).toString());
      }
    })
    .put(new BuiltinFunc("R") { // 'R' is for 'repr'
      public Val calli(Val self, ArrayList<Val> args) {
        expectExactArgumentLength(args, 1);
        return toStr(args.get(0).toString());
      }
    });

public Module readModule(String content, String path) {
  return new Parser(content, path).parse();
}

public void run(final Module node, final String name) {
  push(new Trace() {
    public String getLocationString() {
      return "\nin module " + node.name + " (" + name + ")";
    }
  }, new Scope(GLOBALS));
  try {
    put("__name__", toStr(name));
    node.eval();
  } finally {
    pop();
  }

}

//// Runtime information (stack trace, etc.)
public Simple(String corelib) {
  GLOBALS.put("GLOBAL", new Blob(new HashMap<String, Val>(), GLOBALS.table));
  push(new Trace() {
    public String getLocationString() {
      return "";
    }
  }, GLOBALS);
  run(new Parser(corelib, "<corelib>").parse(), "__corelib__");
}

public static interface Trace {
  public String getLocationString();
}

public final int MAX_RECURSION_DEPTH = 300;
private int DEPTH = 0;
private final Scope[] SCOPE_STACK = new Scope[MAX_RECURSION_DEPTH];
private final Trace[] TRACE_STACK = new Trace[MAX_RECURSION_DEPTH];
private boolean FLAG_RETURN = false;
private boolean FLAG_BREAK = false;
private boolean FLAG_CONTINUE = false;

private boolean jmp() {
  return FLAG_RETURN || FLAG_BREAK || FLAG_CONTINUE;
}

public void push(Trace trace) {
  push(trace, SCOPE_STACK[DEPTH-1]);
}

public void push(Trace trace, Scope scope) {
  if (DEPTH >= MAX_RECURSION_DEPTH)
    throw err(
        "Stack overflow (max recursion depth is set to " +
        Integer.toString(MAX_RECURSION_DEPTH) + ")");
  TRACE_STACK[DEPTH] = trace;
  SCOPE_STACK[DEPTH++] = scope;
}

public void pop() {
  if (DEPTH <= 1)
    throw err("Stack underflow");

  TRACE_STACK[--DEPTH] = null;
  SCOPE_STACK[DEPTH] = null;
}

public Val get(String name) {
  Val val = getScope().getOrNull(name);
  if (val == null)
    throw err("No variabled named '" + name + "'");
  return val;
}

public void put(String name, Val val) {
  getScope().put(name, val);
}

public Scope getScope() {
  return SCOPE_STACK[DEPTH-1];
}

public String getStackTrace() {
  StringBuilder sb = new StringBuilder();
  for (int i = DEPTH-1; i >= 0; i--)
    sb.append(TRACE_STACK[i].getLocationString());
  return sb.toString();
}

//// Val
public abstract class Val {
  public abstract Val searchMetaBlob(String key);
  public abstract boolean equals(Val other);
  public abstract String repr();
  public final boolean equals(Object other) {
    return (other instanceof Val) && equals((Val) other);
  }
  public final Val callMethod(String name) {
    return callMethod(name, new ArrayList<Val>());
  }
  public final Val callMethod(String name, ArrayList<Val> args) {
    Val f = searchMetaBlob(name);
    if (f == null)
      throw noSuchMethodErr(this, name);
    return f.call(this, args);
  }
  public Val bind(Val self) { return this; }
  // TODO: Make toString final and read from metablob.
  public String toString() { return repr(); }
  // TODO: Figure out whether I want to have a special method for truthy,
  // or whether 'truthy' should be sacred where 'nil' and 'false' are the
  // only false values.
  public boolean truthy() { return true; }
  public Val call(Val self, ArrayList<Val> args) {
    return bind(self).call(args);
  }
  public Val call(ArrayList<Val> args) {
    throw err(getClass().getName() + " is not callable");
  }
  public void setattr(String name, Val val) {
    throw err(getClass().getName() + " does not support attribute access");
  }
  public Val getattr(String name) {
    throw err(getClass().getName() + " does not support attribute access");
  }
  public void setitem(Val index, Val val) {
    throw err(getClass().getName() + " does not support item assignment");
  }
}
public final class Nil extends Val {
  public final Val searchMetaBlob(String key) { return MB_NIL.get(key); }
  public final boolean equals(Val other) { return this == other; }
  public final boolean truthy() { return false; }
  public final String repr() { return "nil"; }
}
private abstract class WrapperVal<T> extends Val {
  private final Object val;
  public WrapperVal(T val) { this.val = val; }
  // NOTE: I'm pretty confident here.
  // The check should be done at the one and only constructor.
  // If it doesn't, it means that whoever is using this class is using
  // generics and messing up on their end.
  // You can probably still mess this up with reflection.
  // But if you can, you can also probably do it even if this class didn't
  // use generics.
  @SuppressWarnings("unchecked") public final T getVal() { return (T) val; }
  public final boolean equals(Val other) {
    return
        (other instanceof WrapperVal<?>) &&
        ((WrapperVal<?>) other).val.equals(val);
  }
  public final int hashCode() { return val.hashCode(); }
}
public final class Bool extends WrapperVal<Boolean> {
  public Bool(Boolean val) { super(val); }
  public final Val searchMetaBlob(String key) { return MB_BOOL.get(key); }
  public final boolean truthy() { return getVal(); }
  public final String repr() { return getVal() ? "true" : "false"; }
}
public final class Num extends WrapperVal<Double> {
  public Num(Double val) { super(val); }
  public final Val searchMetaBlob(String key) { return MB_NUM.get(key); }
  public final String repr() {
    double v = getVal();
    return v == Math.floor(v) ?
        Integer.toString((int) v) : Double.toString(v);
  }
}
public final class Str extends WrapperVal<String> {
  public Str(String val) { super(val); }
  public final Val searchMetaBlob(String key) { return MB_STR.get(key); }
  public final String repr() {
    StringBuilder sb = new StringBuilder();
    sb.append("\""); // TODO: Be more thorough
    for (int i = 0; i < getVal().length(); i++) {
      char c = getVal().charAt(i);
      switch (c) {
      case '\"': sb.append("\\\""); break;
      case '\\': sb.append("\\\\"); break;
      default: sb.append(c);
      }
    }
    sb.append("\"");
    return sb.toString();
  }
  public final String toString() { return getVal(); }
}
public final class List extends WrapperVal<ArrayList<Val>> {
  public List(ArrayList<Val> val) { super(val); }
  public final Val searchMetaBlob(String key) { return MB_LIST.get(key); }
  public String repr() {
    StringBuilder sb = new StringBuilder();
    sb.append("L[");
    boolean first = true;
    for (int i = 0; i < getVal().size(); i++) {
      if (first)
        first = false;
      else
        sb.append(", ");
      sb.append(getVal().get(i).repr());
    }
    sb.append("]");
    return sb.toString();
  }
  public final Val get(int i) {
    return getVal().get(i);
  }
  public final Val put(int i, Val val) {
    return getVal().set(i, val);
  }
  public final Val call(ArrayList<Val> args) {
    expectExactArgumentLength(args, 1);
    return get(asNum(args.get(0), "index").getVal().intValue());
  }
  public final void setitem(Val index, Val val) {
    put(asNum(index, "index").getVal().intValue(), val);
  }
}
public final class Map extends WrapperVal<HashMap<Val, Val>> {
  public Map() { super(new HashMap<Val, Val>()); }
  public Map(HashMap<Val, Val> val) { super(val); }
  public final Val searchMetaBlob(String key) { return MB_MAP.get(key); }
  public Val get(Val key) { return getVal().get(key); }
  public Val call(ArrayList<Val> args) {
    expectExactArgumentLength(args, 1);
    return get(args.get(0));
  }
  public String repr() {
    StringBuilder sb = new StringBuilder();
    sb.append("M[");
    Iterator<HashMap.Entry<Val, Val>> it = getVal().entrySet().iterator();
    boolean first = true;
    while (it.hasNext()) {
      if (first)
        first = false;
      else
        sb.append(", ");
      HashMap.Entry<Val, Val> pair = it.next();
      sb.append(pair.getKey().repr() + ", " + pair.getValue().repr());
    }
    sb.append("]");
    return sb.toString();
  }
}
public abstract class Func extends Val {
  public abstract Val call(Val self, ArrayList<Val> args);
  public final Val searchMetaBlob(String key) { return MB_FUNC.get(key); }
  public final Val call(ArrayList<Val> args) { return call(nil, args); }
  public final boolean equals(Val other) { return this == other; }
  public Func bind(Val self) { return new BoundFunc(this, self); }
}
public final class BoundFunc extends Func {
  public final Func f;
  public final Val self;
  public BoundFunc(Func f, Val self) { this.f = f; this.self = self; }
  public final Val call(Val self, ArrayList<Val> args) {
    return f.call(this.self, args);
  }
  public final String repr() { return "<bound func " + f.repr() + ">"; }
  // Binding an already bound function shouldn't change anything.
  public final Func bind(Val self) { return this; }
}
public abstract class BuiltinFunc extends Func implements Trace {
  public final String name;
  public BuiltinFunc(String name) { this.name = name; }
  public final Val call(Val self, ArrayList<Val> args) {
    Simple.this.push(this);
    try {
      return calli(self, args);
    } finally {
      Simple.this.pop();
    }
  }
  public abstract Val calli(Val self, ArrayList<Val> args);
  public final String repr() { return "<builtin func " + name + ">"; }
  public final String getLocationString() {
    return "\nin builtin func '" + name + "'";
  }
}
public final class UserFunc extends Func implements Trace {
  public final Token token;
  public final ArrayList<String> args;
  public final String vararg;
  public final Ast body;
  public final Scope scope;
  public UserFunc(
      Token token, ArrayList<String> args, String vararg, Ast body,
      Scope scope) {
    this.token = token;
    this.args = args;
    this.vararg = vararg;
    this.body = body;
    this.scope = scope;
  }
  public final Val call(Val self, ArrayList<Val> args) {
    Simple.this.push(this, new Scope(scope));
    try {
      Simple.this.put("self", self);
      if (vararg == null) {
        expectExactArgumentLength(args, this.args.size());
      } else {
        expectAtleastArgumentLength(args, this.args.size());
      }
      for (int i = 0; i < this.args.size(); i++)
        Simple.this.put(this.args.get(i), args.get(i));
      if (vararg != null) {
        ArrayList<Val> al = new ArrayList<Val>();
        for (int i = this.args.size(); i < args.size(); i++)
          al.add(args.get(i));
        Simple.this.put(vararg, new List(al));
      }

      Val result = body.eval();

      FLAG_RETURN = false;

      if (FLAG_CONTINUE)
        throw err("'continue' statement outside of 'while'");

      if (FLAG_BREAK)
        throw err("'break' statement outside of 'while'");

      return result;

    } finally {
      Simple.this.pop();
    }
  }
  public final String repr() {
    return
        "<func defined in " + token.lexer.filespec + " line " +
        Integer.toString(token.getLineNumber()) + ">";
  }
  public final String getLocationString() {
    return "\nin user func defined in " + token.getLocationString();
  }
}
public final class Blob extends Val {
  private final HashMap<String, Val> metaBlob;
  private final HashMap<String, Val> attrs;
  public Blob(Blob metaBlob) {
    this(metaBlob.attrs);
  }
  public Blob(HashMap<String, Val> metaBlob) {
    this(metaBlob, new HashMap<String, Val>());
  }
  public Blob(HashMap<String, Val> blob, HashMap<String, Val> attrs) {
    metaBlob = blob;
    this.attrs = attrs;
  }
  public final Val call(ArrayList<Val> args) {
    Val callable = searchMetaBlob("__call__");
    if (callable == null) {
      String name = ""; 
      Val mn = searchMetaBlob("__name__");
      if (mn != null && (mn instanceof Str))
        name += ((Str) mn).getVal();
      else
        name += "<unknown>";
      throw err(name + " type is not callable");
    }
    return searchMetaBlob("__call__").call(this, args);
  }
  public final Val get(String key) {
    Val v = attrs.get(key);
    if (v == null)
      throw err("No attribute '" + key + "'"); // TODO: Add type to message.
    return v;
  }
  public final Blob put(String key, Val val) {
    attrs.put(key, val);
    return this;
  }
  public Blob put(BuiltinFunc bf) {
    return put(bf.name, bf);
  }
  public void setattr(String name, Val val) {
    put(name, val);
  }
  public Val getattr(String name) {
    return get(name);
  }
  public final Val searchMetaBlob(String key) { return metaBlob.get(key); }
  // TODO: Make 'equals' overridable by user.
  public final boolean equals(Val other) { return this == other; }
  // TODO: Make 'hashCode' overridable by user.
  public final int hashCode() { return super.hashCode(); }
  public final String repr() {
    return asStr(callMethod("__repr__"), "repr[]").getVal();
  }
  public final String toString() {
    return asStr(callMethod("__str__"), "str[]").getVal();
  }
}

//// Exceptions

// SyntaxError is thrown if an error is encountered in either
// the lex or parse process.
public static final class SyntaxError extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final Token token;
  public final String message;
  public SyntaxError(Token token, String message) {
    super(message + "\n" + token.getLocationString());
    this.token = token;
    this.message = message;
  }
}

// Err is the general exception thrown whenever we encounter an error
// while a fully parsed program is running.
public final class Err extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final String message;
  public Err(String message) {
    super(message + Simple.this.getStackTrace());
    this.message = message;
  }
}

public Err err(String message) {
  return new Err(message);
}

//// Lexer
public static final class Lexer {
  public static final ArrayList<String> KEYWORDS = toArrayList(
      "and", "or", "xor", "return", "is", "import", "super", "if", "else",
      "while", "def", "class", "not");
  public static final ArrayList<String> SYMBOLS;

  // My syntax highlighter does funny things if it sees "{", "}" in the
  // surrounding scope.
  static {
    SYMBOLS = toArrayList(
        "(", ")", "[", "]", "{", "}", ".", ":", ",", "@",
        "=", "==", "!=", "<", "<=", ">", ">=",
        "+", "-", "*", "/", "%", "\\");
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
  public int getLineNumber() {
    int lc = 1;
    for (int j = 0; j < i; j++)
      if (lexer.string.charAt(j) == '\n')
        lc++;
    return lc;
  }
  public String getLocationString() {
    int a = i, b = i, c = i, lc = getLineNumber();
    while (a > 0 && lexer.string.charAt(a-1) != '\n')
      a--;
    while (b < lexer.string.length() && lexer.string.charAt(b) != '\n')
      b++;

    String spaces = "";
    for (int j = 0; j < i-a; j++)
      spaces = spaces + " ";

    return
        "file '" + lexer.filespec + "' on line " + Integer.toString(lc) +
        "\n" + lexer.string.substring(a, b) + "\n" +
        spaces + "*";
  }
}

//// Parser

public final class Parser {
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
  public Module parse() {

    Token token = peek();
    ArrayList<Ast> exprs = new ArrayList<Ast>();
    while (!at("EOF"))
      exprs.add(parseStatement());

    return new Module(token, name, new BlockAst(token, exprs));
  }
  public Ast parseStatement() {

    if (at("{")) {
      Token token = next();
      ArrayList<Ast> exprs = new ArrayList<Ast>();
      while (!at("}"))
        exprs.add(parseStatement());
      expect("}");
      return new BlockAst(token, exprs);
    }

    if (at("return")) {
      Token token = next();
      Ast value = parseExpression();
      return new ReturnAst(token, value);
    }

    if (at("while")) {
      Token token = next();
      Ast cond = parseExpression();
      Ast body = parseStatement();
      return new WhileAst(token, cond, body);
    }

    if (at("if")) {
      Token token = next();
      Ast cond = parseExpression();
      Ast body = parseStatement();
      Ast other = new BlockAst(token, new ArrayList<Ast>());
      if (consume("else"))
        other = parseStatement();
      return new IfAst(token, cond, body, other);
    }

    return parseExpression();
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
      if (at("<")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new OperationAst(token, node, "__lt__", right);
        continue;
      }
      if (at("<=")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new OperationAst(token, node, "__le__", right);
        continue;
      }
      if (at(">")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new OperationAst(token, node, "__gt__", right);
        continue;
      }
      if (at(">=")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new OperationAst(token, node, "__ge__", right);
        continue;
      }
      if (at("is")) {
        Token token = next();
        if (consume("not")) {
          Ast right = parseAdditiveExpression();
          node = new IsNotAst(token, node, right);
        } else {
          Ast right = parseAdditiveExpression();
          node = new IsAst(token, node, right);
        }
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
    // Negative/positive numeric signs for constants must be
    // handled here because otherwise, we wouldn't be able to
    // distinguish between 'x-1' meaning 'x', '-', '1' or
    // 'x', '-1'.
    if (at("+")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      if (node instanceof NumAst)
        return new NumAst(token, ((NumAst) node).val.getVal());
      else
        return new OperationAst(token, node, "__pos__");
    }
    if (at("-")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      if (node instanceof NumAst)
        return new NumAst(token, -((NumAst) node).val.getVal());
      else
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
        Token token = expect("[");
        ArrayList<Ast> args = new ArrayList<Ast>();
        Ast vararg = null;
        while (!consume("]")) {
          if (consume("*")) {
            vararg = parseExpression();
            expect("]");
            break;
          } else {
            args.add(parseExpression());
            consume(",");
          }
        }

        if (at("=")) {
          token = next();
          if (vararg != null || args.size() != 1)
            throw new SyntaxError(
                token, "For setitem syntax, must have exactly one argument");
          node = new SetItemAst(
              token, node, args.get(0), parseExpression());
        } else {
          node = new CallAst(token, node, args, vararg);
        }
        continue;
      }

      if (at("@")) {
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

      if (at(".")) {
        Token token = next();
        String name = (String) expect("ID").value;
        node = new GetMethodAst(token, node, name);
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
      return new NumAst(token, (Double) token.value);
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

    if (at("\\")) {
      Token token = next();
      ArrayList<String> args = new ArrayList<String>();
      String vararg = null;
      while (at("ID"))
        args.add((String) expect("ID").value);
      if (consume("*"))
        vararg = (String) expect("ID").value;
      consume(".");
      Ast body = parseStatement();
      return new FunctionAst(token, args, vararg, body);
    }

    if (at("if")) {
      Token token = next();
      Ast cond = parseExpression();
      Ast body = parseExpression();
      Ast other = new NameAst(token, "nil");
      if (consume("else"))
        other = parseExpression();
      return new IfAst(token, cond, body, other);
    }

    throw new SyntaxError(peek(), "Expected expression");
  }
}

//// Ast
public abstract class Ast implements Trace {
  public final Token token;
  public Ast(Token token) { this.token = token; }
  public String getLocationString() {
    return "\nin " + token.getLocationString();
  }
  public abstract Val eval();
}
public final class ReturnAst extends Ast {
  public final Ast val;
  public ReturnAst(Token token, Ast val) {
    super(token);
    this.val = val;
  }
  public final Val eval() {
    FLAG_RETURN = true;
    return val.eval();
  }
}
public final class WhileAst extends Ast {
  public final Ast cond;
  public final Ast body;
  public WhileAst(Token token, Ast cond, Ast body) {
    super(token);
    this.cond = cond;
    this.body = body;
  }
  public final Val eval() {
    while (cond.eval().truthy()) {
      Val b = body.eval();
      if (FLAG_CONTINUE) {
        FLAG_CONTINUE = false;
        continue;
      }
      if (FLAG_BREAK) {
        FLAG_BREAK = false;
        break;
      }
      if (FLAG_RETURN)
        return b;
    }
    return nil;
  }
}
public final class BlockAst extends Ast {
  public final ArrayList<Ast> body;
  public BlockAst(Token token, ArrayList<Ast> body) {
    super(token);
    this.body = body;
  }
  public final Val eval() {
    for (int i = 0; i < body.size(); i++) {
      Val v = body.get(i).eval();
      if (FLAG_BREAK||FLAG_CONTINUE||FLAG_RETURN)
        return v;
    }
    return nil;
  }
}
public final class NumAst extends Ast {
  public final Num val;
  public NumAst(Token token, Double val) {
    super(token);
    this.val = toNum(val);
  }
  public final Val eval() { return val; }
}
public final class StringAst extends Ast {
  public final Str val;
  public StringAst(Token token, String val) {
    super(token);
    this.val = toStr(val);
  }
  public final Val eval() { return val; }
}
public final class NameAst extends Ast {
  public final String name;
  public NameAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public final Val eval() {
    push(this);
    try {
      return Simple.this.get(name);
    } finally {
      pop();
    }
  }
}
public final class IfAst extends Ast {
  public final Ast cond, body, other;
  public IfAst(
      Token token, Ast cond, Ast body, Ast other) {
    super(token);
    this.cond = cond;
    this.body = body;
    this.other = other;
  }
  public final Val eval() {
    return cond.eval().truthy() ? body.eval() : other.eval();
  }
}
public final class AssignAst extends Ast {
  public final String name;
  public final Ast val;
  public AssignAst(Token token, String name, Ast val) {
    super(token);
    this.name = name;
    this.val = val;
  }
  public final Val eval() {
    Val v = val.eval();
    Simple.this.put(name, v);
    return v;
  }
}
public final class FunctionAst extends Ast {
  public final ArrayList<String> args;
  public final String vararg;
  public final Ast body;
  public FunctionAst(
      Token token, ArrayList<String> args, String vararg, Ast body) {
    super(token);
    this.args = args;
    this.vararg = vararg;
    this.body = body;
  }
  public final Val eval() {
    return new UserFunc(token, args, vararg, body, getScope());
  }
}
public final class CallAst extends Ast {
  public final Ast f;
  public final ArrayList<Ast> args;
  public final Ast vararg;
  public CallAst(
      Token token, Ast f,
      ArrayList<Ast> args, Ast vararg) {
    super(token);
    this.f = f;
    this.args = args;
    this.vararg = vararg;
  }
  public final Val eval() {
    Val vf = f.eval();
    ArrayList<Val> va = new ArrayList<Val>();
    for (int i = 0; i < args.size(); i++)
      va.add(args.get(i).eval());
    if (vararg != null) {
      ArrayList<Val> vl = asList(vararg.eval(), "vararg").getVal();
      for (int i = 0; i < vl.size(); i++)
        va.add(vl.get(i));
    }
    Simple.this.push(this);
    try {
      return vf.call(va);
    } finally {
      Simple.this.pop();
    }
  }
}
public final class GetMethodAst extends Ast {
  public final Ast owner;
  public final String name;
  public GetMethodAst(Token token, Ast owner, String name) {
    super(token);
    this.owner = owner;
    this.name = name;
  }
  public final Val eval() {
    Val v = owner.eval();
    Val f = v.searchMetaBlob(name);
    if (f == null) {
      push(this);
      try {
        throw noSuchMethodErr(v, name);
      } finally {
        pop();
      }
    }
    return f.bind(v);
  }
}
public final class SetItemAst extends Ast {
  public final Ast owner, index, value;
  public SetItemAst(
      Token token, Ast owner, Ast index, Ast value) {
    super(token);
    this.owner = owner;
    this.index = index;
    this.value = value;
  }
  public final Val eval() {
    Val self = owner.eval();
    Val ind = index.eval();
    Val val = value.eval();
    push(this);
    try {
      self.setitem(ind, val);
      return val;
    } finally {
      pop();
    }
  }
}
public final class GetAttributeAst extends Ast {
  public final Ast owner;
  public final String name;
  public GetAttributeAst(Token token, Ast owner, String name) {
    super(token);
    this.owner = owner;
    this.name = name;
  }
  public final Val eval() {
    Val self = owner.eval();
    push(this);
    try {
      return self.getattr(name);
    } finally {
      pop();
    }
  }
}
public final class SetAttributeAst extends Ast {
  public final Ast owner;
  public final String name;
  public final Ast val;
  public SetAttributeAst(
      Token token, Ast owner, String name, Ast val) {
    super(token);
    this.owner = owner;
    this.name = name;
    this.val = val;
  }
  public final Val eval() {
    Val self = owner.eval();
    Val v = val.eval();
    push(this);
    try {
      self.setattr(name, v);
      return v;
    } finally {
      pop();
    }
  }
}
public final class IsAst extends Ast {
  public final Ast left, right;
  public IsAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Val eval() {
    return left.eval() == right.eval() ? tru : fal;
  }
}
public final class IsNotAst extends Ast {
  public final Ast left, right;
  public IsNotAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Val eval() {
    return left.eval() != right.eval() ? tru : fal;
  }
}
public final class OperationAst extends Ast {
  public final Ast owner;
  public final String name;
  public final ArrayList<Ast> args;
  public OperationAst(
      Token token, Ast owner, String name, Ast... args) {
    super(token);
    this.owner = owner;
    this.name = name;
    this.args = toArrayList(args);
  }
  public final Val eval() {
    Val v = owner.eval();
    ArrayList<Val> al = new ArrayList<Val>();
    for (int i = 0; i < args.size(); i++)
      al.add(args.get(i).eval());
    Simple.this.push(this);
    try {
      return v.callMethod(name, al);
    } finally {
      Simple.this.pop();
    }
  }
}
public final class NotAst extends Ast {
  public final Ast target;
  public NotAst(Token token, Ast target) {
    super(token);
    this.target = target;
  }
  public final Val eval() {
    return target.eval().truthy() ? fal : tru;
  }
}
public final class AndAst extends Ast {
  public final Ast left, right;
  public AndAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Val eval() {
    Val l = left.eval();
    return l.truthy() ? right.eval() : l;
  }
}
public final class OrAst extends Ast {
  public final Ast left, right;
  public OrAst(Token token, Ast left, Ast right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public final Val eval() {
    Val l = left.eval();
    return l.truthy() ? l : right.eval();
  }
}
public final class Module extends Ast {
  public final String name;
  public final Ast body;
  public Module(Token token, String name, Ast body) {
    super(token);
    this.name = name;
    this.body = body;
  }
  public final Val eval() {
    return body.eval();
  }
}
//// Scope

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
  public Scope put(BuiltinFunc bf) {
    return put(bf.name, bf);
  }
  public Scope put(Map m) {
    String n = asStr(m.getVal().get(toStr("__name__")), "FUBAR").getVal();
    return put(n, m);
  }
}

//// Utils
public static final class BlobHashMapFactory {
  public final HashMap<String, Val> val = new HashMap<String, Val>();
  public BlobHashMapFactory put(String k, Val v) {
    val.put(k, v);
    return this;
  }
  public BlobHashMapFactory put(BuiltinFunc bf) {
    return put(bf.name, bf);
  }
  public HashMap<String, Val> get() { return val; }
}
public static ArrayList<Val> toArrayList(Val... args) {
  ArrayList<Val> al = new ArrayList<Val>();
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
public static String filespecToName(String filespec) {
  int start, end = filespec.length();
  for (start = filespec.length()-1;
        start >= 1 && filespec.charAt(start-1) != '/' &&
        filespec.charAt(start-1) != '\\'; start--);
  if (filespec.endsWith(".ccl"))
    end -= ".ccl".length();
  return filespec.substring(start, end);
}

public void expectExactArgumentLength(ArrayList<Val> args, int len) {
  if (args.size() != len)
    throw err(
        "Expected " + Integer.toString(len) + " arguments but found " +
        Integer.toString(args.size()));
}

public void expectAtleastArgumentLength(ArrayList<Val> args, int len) {
  if (args.size() < len)
    throw err(
        "Expected at least " + Integer.toString(len) +
        " arguments but found " + Integer.toString(args.size()));
}

public Err noSuchMethodErr(Val self, String name) {
  String message = "No method '" + name + "' found for type ";
  Val mn = self.searchMetaBlob("__name__");
  if (mn != null && (mn instanceof Str))
    message += ((Str) mn).getVal();
  else
    message += "<unknown>";
  return err(message);
}

public Num asNum(Val v, String name) {
  if (!(v instanceof Num))
    throw err(
        "Expected " + name + " to be Num but found " +
        v.getClass().getName());
  return (Num) v;
}

public Str asStr(Val v, String name) {
  if (!(v instanceof Str))
    throw err(
        "Expected " + name + " to be Str but found " +
        v.getClass().getName());
  return (Str) v;
}

public Str asStr(Val v, int i) {
  if (!(v instanceof Str))
    throw err(
        "Expected argument " + Integer.toString(i) + " to be Str but found " +
        v.getClass().getName());
  return (Str) v;
}

public List asList(Val v, String name) {
  if (!(v instanceof List))
    throw err(
        "Expected " + name + " to be List but found " +
        v.getClass().getName());
  return (List) v;
}

public Func asFunc(Val v, String name) {
  if (!(v instanceof Func))
    throw err(
        "Expected " + name + " to be Func but found " +
        v.getClass().getName());
  return (Func) v;
}

public Blob asBlob(Val v, String name) {
  if (!(v instanceof Blob))
    throw err(
        "Expected " + name + " to be Blob but found " +
        v.getClass().getName());
  return (Blob) v;
}

public Num toNum(Double value) {
  return new Num(value);
}

public Num toNum(Integer value) {
  return new Num(value.doubleValue());
}

public Str toStr(String value) {
  return new Str(value);
}

public List toList(ArrayList<Val> value) {
  return new List(value);
}

}
