import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class S {

/// Val
public static final Nil nil = new Nil();
public static final Bool tru = new Bool(true);
public static final Bool fal = new Bool(false);
public abstract static class Val {}
public static final class Nil extends Val {}
public abstract static class WrVal<T> extends Val {
  private final Object val;
  public WrVal(T val) { this.val = val; }
  @SuppressWarnings("unchecked") public final T getVal() { return (T) val; }
  public final boolean equals(Val other) {
    return
        (other instanceof WrVal<?>) &&
        ((WrVal<?>) other).val.equals(val);
  }
}
public static final class Bool extends WrVal<Boolean> {
  public Bool(Boolean val) { super(val); }
}
public static final class Num extends WrVal<Double> {
  public Num(Double val) { super(val); }
  public static Num g(Double s) { return new Num(s); }
}
public static final class Str extends WrVal<String> {
  public Str(String val) { super(val); }
  public static Str g(String s) { return new Str(s); }
}

/// Parser
public static final class Module {
  public final Token token;
  private final ArrayList<Bytecode> buf;
  public Module(Token token, ArrayList<Bytecode> buf) {
    this.token = token;
    this.buf = buf;
  }
  public Bytecode get(int i) { return buf.get(i); }
  public int size() { return buf.size(); }
}

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
  public Module parse() {
    Token token = peek();
    ArrayList<Bytecode> buf = new ArrayList<Bytecode>();
    while (!at("EOF"))
      parseExpression(buf);
    return new Module(token, buf);
  }

  public void parseExpression(ArrayList<Bytecode> buf) {
    parsePrimaryExpression(buf);
  }

  public void parsePrimaryExpression(ArrayList<Bytecode> buf) {
    if (at("nil")) {
      buf.add(new LiteralBytecode(next(), nil));
      return;
    }
    if (at("true")) {
      buf.add(new LiteralBytecode(next(), tru));
      return;
    }
    if (at("false")) {
      buf.add(new LiteralBytecode(next(), fal));
      return;
    }
    if (at("STR")) {
      Token token = next();
      buf.add(new LiteralBytecode(token, Str.g((String) token.value)));
    }
    if (at("NUM")) {
      Token token = next();
      buf.add(new LiteralBytecode(token, Num.g((Double) token.value)));
    }
    if (at("ID")) {
      Token token = next();
      String name = (String) token.value;
      buf.add(new NameBytecode(token, (String) token.value));
    }
    throw new SyntaxError(peek(), "Expected expression");
  }
}

/// State
public static final class State {
  public final Scope sc;
  public int ip = 0;
  public boolean ret = false;
  private final Module m;
  private ArrayList<Val> vs = new ArrayList<Val>();
  public State(Module p, int i, Scope s) { m = p; ip = i; sc = s; }
  public Val run() {
    while (ip < m.size() && !ret)
      m.get(ip++).step(this);
    return vs.isEmpty() ? nil : vs.remove(vs.size()-1);
  }
  public void push(Val v) { vs.add(v); }
  public Val pop(Val v) { return vs.remove(vs.size()-1); }
  public Val get(String name) {
    Val v = sc.getOrNull(name);
    if (v == null)
      throw err(this, "No variable named '" + name + "' in scope");
    return v;
  }
}

/// Bytecode
public abstract static class Bytecode {
  public final Token token;
  public Bytecode(Token token) { this.token = token; }
  public abstract void step(State state);
}

public static final class LiteralBytecode extends Bytecode {
  public final Val val;
  public LiteralBytecode(Token token, Val v) { super(token); val = v; }
  public final void step(State state) { state.push(val); }
}

public static final class NameBytecode extends Bytecode {
  public final String val;
  public NameBytecode(Token token, String v) { super(token); val = v; }
  public final void step(State state) { state.push(state.get(val)); }
}

/// Lexer
public static final class Lexer {
  public static final ArrayList<String> KEYWORDS = toArrayList(
      "and", "or", "xor", "return", "is", "import", "super", "if", "else",
      "while", "def", "class", "not", "nil", "true", "false");
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

/// Scope
public static final class Scope {
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
}

/// Utils

public static RuntimeException err(State s, String message) {
  return new RuntimeException(message);
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

}
