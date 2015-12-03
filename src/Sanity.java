import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Sanity {

/// Flag indicating whether we should perform tests on startup.
public static final boolean TEST = true;

// Tests are run in static blocks so we don't need to load them here.
public static void main(String[] args) {
  if (TEST)
    System.out.println("All tests pass!");
}

public static HashMap<String, Value> run(ModuleAst node, String name) {
  Context c = new Context();
  c.put("__name__", new StringValue(name));
  return run(c, node);
}

public static HashMap<String, Value> run(Context c, ModuleAst node) {
  c.checkStart();
  node.eval(c);
  c.checkEnd();
  return c.scope.table;
}

public static UserValue importModule(ModuleAst node) {
  HashMap<String, Value> table = run(node, node.name);
  UserValue value = new UserValue(typeModule);

  Iterator<String> it = table.keySet().iterator();
  while (it.hasNext()) {
    String key = it.next();
    value.put(key, table.get(key));
  }

  return value;
}

/// Effectively the core library.

public static final NilValue nil = new NilValue();
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
public static final TypeValue typeModule = new TypeValue("Module", typeValue);

public static final Scope BUILTIN_SCOPE = new Scope(null)
    .put("nil", nil);

public static final class NilValue extends Value {
  public final TypeValue getType() { return typeNil; }
}
public static final class NumberValue extends Value {
  public final Double value;
  public NumberValue(Double value) { this.value = value; }
  public final TypeValue getType() { return typeNumber; }
}
public static final class StringValue extends Value {
  public final String value;
  public StringValue(String value) { this.value = value; }
  public final TypeValue getType() { return typeString; }
}

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

  // For variable lookups.
  public Scope scope = new Scope(BUILTIN_SCOPE);

  // Verify the snaity of Context
  public final void checkStart() {
    if (ret)
      throw err(this, "ret is set");
    if (br)
      throw err(this, "br is set");
    if (cont)
      throw err(this, "cont is set");
  }
  public final void checkEnd() {
    if (value == null)
      throw err(this, "val is null");
  }

  // Indicates whether any of the special control flow flags are set.
  public final boolean jump() {
    return ret || br || cont;
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
      throw err(c, "No such method " + name);

    Trace oldTrace = c.trace;
    try {
      c.trace = new CallTrace(c.trace, ownerType, methodType, method);
      c.checkStart();
      method.call(c, this, args);
      c.checkEnd();
    } finally {
      c.trace = oldTrace;
    }
  }

  // Only UserValue should override this method.
  // This method should be considered final for all other purposes.
  public Value get(Context c, String name) {
    for (int i = 0; i < getType().mro.size(); i++) {
      TypeValue ancestor = getType().mro.get(i);
      Method method = ancestor.methods.get(name);
      if (method != null)
        return new BoundMethodValue(this, method);
    }
    throw err(c,
        "No attribute named " + name +
        " for value of instance " + getType().name);
  }

  // TODO: Figure out what to do about these java bridge methods.
  // The problem is that I can't pass a 'Context' argument so when there is a
  // problem I can't get a full stack trace.
  public final boolean equals(Object x) { throw new Fubar(); }
  public final int hashCode() { throw new Fubar(); }
  public final String toString() { throw new Fubar(); }
}

public static final class TypeValue extends Value {
  public final String name;
  public final ArrayList<TypeValue> bases = new ArrayList<TypeValue>();
  public final ArrayList<TypeValue> mro = new ArrayList<TypeValue>();
  public final HashMap<String, Method> methods = new HashMap<String, Method>();
  public final TypeValue getType() { return typeType; }
  public TypeValue(String n, ArrayList<Value> bs) {
    if (n == null)
      throw new Fubar();

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

public static final class UserValue extends Value {
  public final TypeValue type;
  public final HashMap<String, Value> attrs = new HashMap<String, Value>();
  public UserValue(TypeValue type) { this.type = type; }
  public UserValue put(String name, Value value) {
    attrs.put(name, value);
    return this;
  }
  public final TypeValue getType() { return type; }
  public final Value get(Context c, String name) {
    Value value = attrs.get(name);
    return value == null ? super.get(c, name) : value;
  }
}

public abstract static class FunctionValue extends Value {
  public final String name;
  public FunctionValue(String name) { this.name = name; }
  public final TypeValue getType() { return typeFunction; }
  public abstract void calli(Context c, ArrayList<Value> args);
  public final void call(Context c, ArrayList<Value> args) {
    c.checkStart();
    calli(c, args);
    c.checkEnd();
  }
}

public static final class BoundMethodValue extends FunctionValue {
  public final Value owner;
  public final Method method;
  public BoundMethodValue(Value owner, Method method) {
    super(method.name);
    this.owner = owner;
    this.method = method;
  }
  public final void calli(Context c, ArrayList<Value> args) {
    method.call(c, owner, args);
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

/// Ast

public abstract static class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }
  public abstract void evali(Context c);
  public final void eval(Context c) {
    c.checkStart();
    evali(c);
    c.checkEnd();
  }
}

public static final class NameAst extends Ast {
  public final String name;
  public NameAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public final void evali(Context c) {
    c.value = c.get(name);
  }
}

public static final class StringAst extends Ast {
  public final StringValue value;
  public StringAst(Token token, String value) {
    super(token);
    this.value = new StringValue(value);
  }
  public final void evali(Context c) {
    c.value = value;
  }
}

public static final class NumberAst extends Ast {
  public final NumberValue value;
  public NumberAst(Token token, Double value) {
    super(token);
    this.value = new NumberValue(value);
  }
  public final void evali(Context c) {
    c.value = value;
  }
}

public static final class BlockAst extends Ast {
  public final ArrayList<Ast> body;
  public BlockAst(Token token, ArrayList<Ast> body) {
    super(token);
    this.body = body;
  }
  public final void evali(Context c) {
    c.value = nil;
    for (int i = 0; i < body.size(); i++) {
      body.get(i).eval(c);
      if (c.jump())
        return;
    }
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
  public final void evali(Context c) {
    body.eval(c);
  }
}

// Ast test

static {
  if (TEST) {
    Context c = new Context();

    run(c, new Parser("5", "<test>").parse());
    expect(c.value instanceof NumberValue);
    expect(((NumberValue) c.value).value.equals(5.0));

    System.out.println("Ast tests pass");
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
    return parsePrimaryExpression();
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
      return new NameAst(token, (String) token.value);
    }

    if (consume("(")) {
      Ast expr = parseExpression();
      expect(")");
      return expr;
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

    System.out.println("Parser tests pass");
  }
}

/// Lexer and Token

public static final class Lexer {
  public static final ArrayList<String> KEYWORDS = toArrayList("def");
  public static final ArrayList<String> SYMBOLS;

  // My syntax highlighter does funny things if it sees "{", "}" in the
  // surrounding scope.
  static {
    SYMBOLS = toArrayList(
        "(", ")", "[", "]", ".", ":",
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

    System.out.println("Lexer tests pass");
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
  if (!cond) throw new Fubar();
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

}
