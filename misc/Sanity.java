package com.ccl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public final class Sanity {

/// Tier 1: Eval utils
public static final class Scope {
  public final HashMap<String, Object> table = new HashMap<String, Object>();
  public final Scope parent;
  public Scope(Scope parent) { this.parent = parent; }
  public Object getOrNull(String key) {
    Object value = table.get(key);
    if (value != null)
      return value;
    if (parent != null)
      return parent.getOrNull(key);
    return null;
  }
  public Object get(String key) {
    Object value = getOrNull(key);
    if (value == null)
      throw new Err("No such name: " + key);
    return value;
  }
  public Scope put(String key, Object value) {
    table.put(key, value);
    return this;
  }
}

public static final class Nil {
  public static final Nil value = new Nil();
  private Nil() {}
}

public abstract static class Function {
  public abstract Object call(Object self, ArrayList<?> arguments);
}

public static final Scope META_VALUE = new Scope(null);
public static final Scope META_INT = new Scope(META_VALUE);

public static boolean truthy(Object value) {
  return !value.equals(false);
}

public static Scope getMeta(Object value) {
  if (value instanceof BigInteger) {
    return META_INT;
  } else if (value instanceof Scope) {
    Scope meta = (Scope) ((Scope) value).getOrNull("__meta__");
    return meta == null ? META_VALUE : meta;
  } else {
    throw new Err("Unsupported java type: " + value.getClass().getName());
  }
}

public static Function getMethod(Object self, String name) {
  return (Function) getMeta(self).get(name);
}

public static Object callMethod(
    Object self, String name, ArrayList<?> arguments) {
  return getMethod(self, name).call(self, arguments);
}

public interface Traceable {
  public String getTraceMessage();
}

public static final class Err extends RuntimeException {
  public static final long serialVersionUID = 42L;

  private final ArrayList<Traceable> trace = new ArrayList<Traceable>();
  public Err(Throwable exc) {
    super(exc);
  }
  public Err(String message) {
    super(message);
  }
  public void add(Traceable tr) {
    trace.add(tr);
  }
  public String getTraceString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < trace.size(); i++)
      sb.append(trace.get(i).getTraceMessage());
    return sb.toString();
  }
}

public static <T> T notNull(T t) {
  if (t == null)
    throw new Err("Unexpected null pointer");
  return t;
}

public static void expectArgRange(ArrayList<?> args, int min, int max) {
  if (args.size() < min || args.size() > max)
    throw new Err(
        "Expected " + min + " to " + max + " arguments but got " +
        args.size() + ".");
}

public static void expectArglen(ArrayList<?> args, int len) {
  if (args.size() != len)
    throw new Err(
        "Expected " + len + " arguments but got " +
        args.size() + ".");
}

public static void expectMinArglen(ArrayList<?> args, int len) {
  if (args.size() < len)
    throw new Err(
        "Expected at least " + len + " arguments but got only " +
        args.size() + ".");
}

public static void expectArglens(ArrayList<?> args, int... lens) {
  for (int i = 0; i < lens.length; i++)
    if (args.size() == lens[i])
      return;
  StringBuilder sb = new StringBuilder("Expected ");
  for (int i = 0; i < lens.length-1; i++)
    sb.append(lens[i] + " or ");
  sb.append(lens[lens.length-1] + " arguments but found " + args.size());
  throw new Err(sb.toString());
}

/// Tier 2: Lexer
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

public static final class Lexer {
  public static final ArrayList<String> KEYWORDS = toArrayList(
      "and", "or", "xor", "return", "is", "super", "let",
      "if", "then", "else", "while", "not", "break", "continue");
  public static final ArrayList<String> SYMBOLS;

  // My syntax highlighter does funny things if it sees "{", "}" in the
  // surrounding scope.
  static {
    SYMBOLS = toArrayList(
        "(", ")", "[", "]", "{", "}", ".", ":", ",", "@",
        "=", "==", "!=", "<", "<=", ">", ">=",
        "+", "-", "*", "/", "//", "%", "\\", "\\\\");
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
          case 'n': sb.append('\n'); break;
          case 't': sb.append('\t'); break;
          case '"': sb.append('\"'); break;
          case '\'': sb.append('\''); break;
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
        seenDot = true;
        pos++;
        while (isdigit())
          pos++;
      }
      if (seenDot)
        return makeToken(start, "FLT", Double.valueOf(cut(start)));
      else
        return makeToken(start, "INT", new BigInteger(cut(start)));
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

  public static ArrayList<String> toArrayList(String... args) {
    ArrayList<String> al = new ArrayList<String>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }
}

/// Tier 3: Ast
public abstract static class Pattern {
  public abstract void assign(Scope scope, Object value);
}
public static final class NamePattern extends Pattern {
  public final String name;
  public NamePattern(String name) { this.name = name; }
  public void assign(Scope scope, Object value) {
    scope.put(name, value);
  }
}
public static final class ListPattern extends Pattern {
  public final ArrayList<Pattern> arguments;
  public final ArrayList<String> optionals;
  public final String vararg; // nullable
  public ListPattern(
      ArrayList<Pattern> arguments, ArrayList<String> optionals,
      String vararg) {
    this.arguments = arguments;
    this.optionals = optionals;
    this.vararg = vararg;
  }
  public void assign(Scope scope, Object rawValue) {
    if (!(rawValue instanceof ArrayList))
      throw new Err("Pattern assign on a non-list not yet supported");
    ArrayList<?> value = (ArrayList) rawValue;
    throw new Err("Not yet implemented");
  }
}
public abstract static class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }
}
public abstract static class Statement extends Ast {
  public Statement(Token token) { super(token); }
  // If statement ran normally returns null.
  // If returns a 'Break' or 'Continue' object we are breaking or
  // continuing.
  // Otherwise we are returning from function.
  public abstract Object run(Scope scope);
}
public static final class BlockStatement extends Statement {
  public final ArrayList<Statement> statements;
  public BlockStatement(Token token, ArrayList<Statement> statements) {
    super(token);
    this.statements = statements;
  }
  public Object run(Scope scope) {
    for (int i = 0; i < statements.size(); i++) {
      Object value = statements.get(i);
      if (value != null)
        return value;
    }
    return null;
  }
}
public static final class WhileStatement extends Statement {
  public final Expression condition;
  public final Statement body;
  public WhileStatement(Token token, Expression condition, Statement body) {
    super(token);
    this.condition = condition;
    this.body = body;
  }
  public Object run(Scope scope) {
    while (truthy(condition.eval(scope))) {
      Object value = body.run(scope);
      if (value instanceof Break)
        break;
      if (value instanceof Continue)
        continue;
      if (value != null)
        return value;
    }
    return null;
  }
}
public static final class Break extends Statement {
  public Break(Token token) { super(token); }
  public Object run(Scope scope) { return this; }
}
public static final class Continue extends Statement {
  public Continue(Token token) { super(token); }
  public Object run(Scope scope) { return this; }
}
public static final class IfStatement extends Statement {
  public final Expression condition;
  public final Statement body;
  public final Statement other; // nullable
  public IfStatement(
      Token token, Expression condition, Statement body, Statement other) {
    super(token);
    this.condition = condition;
    this.body = body;
    this.other = other;
  }
  public Object run(Scope scope) {
    if (truthy(condition.eval(scope)))
      return body.run(scope);
    else if (other != null)
      return other.run(scope);
    return null;
  }
}
public static final class ReturnStatement extends Statement {
  public final Expression expression;
  public ReturnStatement(Token token, Expression expression) {
    super(token);
    this.expression = expression;
  }
  public Object run(Scope scope) {
    return expression.eval(scope);
  }
}
public static final class ExpressionStatement extends Statement {
  public final Expression expression;
  public ExpressionStatement(Token token, Expression expression) {
    super(token);
    this.expression = expression;
  }
  public Object run(Scope scope) {
    expression.eval(scope);
    return null;
  }
}
public abstract static class Expression extends Ast {
  public Expression(Token token) { super(token); }
  // Should never return null.
  public abstract Object eval(Scope scope);
}
public static final class IntExpression extends Expression {
  public final BigInteger value;
  public IntExpression(Token token, BigInteger value) {
    super(token);
    this.value = value;
  }
  public Object eval(Scope scope) { return value; }
}
public static final class FloatExpression extends Expression {
  public final Double value;
  public FloatExpression(Token token, Double value) {
    super(token);
    this.value = value;
  }
  public Object eval(Scope scope) { return value; }
}
public static final class StringExpression extends Expression {
  public final String value;
  public StringExpression(Token token, String value) {
    super(token);
    this.value = value;
  }
  public Object eval(Scope scope) { return value; }
}
public static final class FunctionExpression extends Expression {
  public final ListPattern arguments;
  public final Statement body;
  public FunctionExpression(
      Token token, ListPattern arguments, Statement body) {
    super(token);
    this.arguments = arguments;
    this.body = body;
  }
  public Object eval(final Scope scope) {
    return new Function() {
      public Object call(Object self, ArrayList<?> arguments) {
        scope.put("self", self);
        FunctionExpression.this.arguments.assign(scope, arguments);
        Object result = body.run(scope);
        return result == null ? Nil.value : result;
      }
    };
  }
}
public static final class NameExpression extends Expression {
  public final String name;
  public NameExpression(Token token, String name) {
    super(token);
    this.name = name;
  }
  public Object eval(Scope scope) { return scope.get(name); }
}
public static final class AssignExpression extends Expression {
  public final Pattern pattern;
  public final Expression expression;
  public AssignExpression(
      Token token, Pattern pattern, Expression expression) {
    super(token);
    this.pattern = pattern;
    this.expression = expression;
  }
  public Object eval(Scope scope) {
    Object value = expression.eval(scope);
    pattern.assign(scope, value);
    return value;
  }
}
public static final class CallExpression extends Expression {
  public final Expression owner;
  public final String name;
  public final ArrayList<Expression> arguments;
  public final Expression vararg; // nullable
  public CallExpression(
      Token token, Expression owner, String name,
      ArrayList<Expression> arguments, Expression vararg) {
    super(token);
    this.owner = owner;
    this.name = name;
    this.arguments = arguments;
    this.vararg = vararg;
  }
  public Object eval(Scope scope) {
    // TODO
    return null;
  }
}
public static final class IfExpression extends Expression {
  public final Expression condition, body, other;
  public IfExpression(
      Token token, Expression condition, Expression body, Expression other) {
    super(token);
    this.condition = condition;
    this.body = body;
    this.other = other;
  }
  public Object eval(Scope scope) {
    return (truthy(condition.eval(scope)) ? body : other).eval(scope);
  }
}
public static final class OrExpression extends Expression {
  public final Expression left, right;
  public OrExpression(Token token, Expression left, Expression right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public Object eval(Scope scope) {
    Object value = left.eval(scope);
    if (truthy(value))
      return value;
    return right.eval(scope);
  }
}
public static final class AndExpression extends Expression {
  public final Expression left, right;
  public AndExpression(Token token, Expression left, Expression right) {
    super(token);
    this.left = left;
    this.right = right;
  }
  public Object eval(Scope scope) {
    Object value = left.eval(scope);
    if (!truthy(value))
      return value;
    return right.eval(scope);
  }
}

}
