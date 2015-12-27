// Simpler!!!!
// Inheritance logic, yuck.
// Lua style Metatable approach might be better.
// Also with respect to context...
// I feel like it would be cleaner to have a separate
// context object that could remain orthogonal to any 'Val' instance,
// but Ugh. That means I have to keep track of extra Context arg everywhere.
// Just use 'Simple' instance as global context. At least that feels
// a little less bad than actually using globals.
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class Simple {

public static void main(String[] args) {
  new Simple().xmain(args);
}

public void xmain(String[] args) {
  ModuleAst mainModule = readModule(args[0]);
  run(mainModule, "__main__");
}

public ModuleAst readModule(String path) {
  return new Parser(readFile(path), path).parse();
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
    throw new RuntimeException(
        "Exception while reading " + path + ": " + e.toString());
  }
}

public void run(ModuleAst node, String name) {
  push(new ModuleFrame(new Scope(GLOBALS), node, name));
  try {
    put("__name__", new Str(name));
    node.eval();
  } finally {
    pop();
  }

}

//// Runtime globals

public final Scope GLOBALS = new Scope(null)
    .put(new BuiltinFunc("Print") {
      public Val calli(Val self, ArrayList<Val> args) {
        System.out.println(args.get(0));
        return args.get(0);
      }
    });

public final int MAX_RECURSION_DEPTH = 2000;
private final Frame[] STACK = new Frame[MAX_RECURSION_DEPTH];
private int DEPTH = 0;
private boolean FLAG_RETURN = false;
private boolean FLAG_BREAK = false;
private boolean FLAG_CONTINUE = false;

private boolean jmp() {
  return FLAG_RETURN || FLAG_BREAK || FLAG_CONTINUE;
}

public void push(Frame frame) {
  if (DEPTH >= MAX_RECURSION_DEPTH)
    throw new RuntimeException("Stack overflow");
  STACK[DEPTH++] = frame;
}

public void pop() {
  if (DEPTH == 0)
    throw new RuntimeException("Stack underflow");
  STACK[--DEPTH] = null;
}

public Val get(String name) {
  Val val = getFrame().scope.getOrNull(name);
  if (val == null)
    throw new Err("No variabled named '" + name + "'");
  return val;
}

public void put(String name, Val val) {
  getFrame().scope.put(name, val);
}

public Frame getFrame() {
  return STACK[DEPTH-1];
}

public String getLocationString() {
  StringBuilder sb = new StringBuilder();
  for (int i = 0; i < DEPTH; i++)
    sb.append(STACK[i].getLocationString());
  return sb.toString();
}

public static abstract class Frame {
  public final Scope scope;
  public Frame(Scope scope) { this.scope = scope; }
  public abstract String getLocationString();
}

public static final class ModuleFrame extends Frame {
  public final ModuleAst module;
  public final String moduleName;
  public ModuleFrame(Scope scope, ModuleAst module, String moduleName) {
    super(new Scope(scope));
    this.module = module;
    this.moduleName = moduleName;
  }
  public final String getLocationString() {
    return "\nin module " + module.name + " (" + moduleName + ")";
  }
}

public static final class UserFuncFrame extends Frame {
  public final Token token;
  public UserFuncFrame(Scope scope, Token token) {
    super(new Scope(scope));
    this.token = token;
  }
  public final String getLocationString() {
    return "\nin user function defined in " + token.getLocationString();
  }
}

public final class AstFrame extends Frame {
  public final Token token;
  public AstFrame(Token token) {
    super(Simple.this.getFrame().scope);
    this.token = token;
  }
  public final String getLocationString() {
    return "\nin expression in " + token.getLocationString();
  }
}

public final class BuiltinFuncFrame extends Frame {
  public final String name;
  public BuiltinFuncFrame(String name) {
    super(Simple.this.getFrame().scope);
    this.name = name;
  }
  public final String getLocationString() {
    return "\nin builtin function in " + name;
  }
}

//// Val

// Metamaps for builtin types.
public final Nil nil = new Nil();
public final Bool tru = new Bool(true);
public final Bool fal = new Bool(false);
public final Map MM_NIL = new Map();
public final Map MM_BOOL = new Map();
public final Map MM_NUM = new Map()
    .put(new BuiltinFunc("__add__") {
      public Val calli(Val self, ArrayList<Val> args) {
        return new Num(((Num) self).getVal() + ((Num) args.get(0)).getVal());
      }
    });
public final Map MM_STR = new Map();
public final Map MM_LIST = new Map();
public final Map MM_MAP = new Map();
public final Map MM_FUNC = new Map();
public abstract class Val {
  public abstract Map getMetaMap();
  public abstract boolean equals(Val other);
  public final boolean equals(Object other) {
    return (other instanceof Val) && equals((Val) other);
  }
  public final Val callMethod(Str name, ArrayList<Val> args) {
    Val f = getMetaMap().getVal().get(name);
    if (f == null || !(f instanceof Func))
      throw new Err("No method named " + name.getVal());
    return ((Func) f).call(this, args);
  }
  public boolean truthy() {
    return true;
  }
}
public final class Nil extends Val {
  public final Map getMetaMap() { return MM_NIL; }
  public final boolean equals(Val other) { return this == other; }
  public final boolean truthy() { return false; }
}
private abstract class WrapperVal<T> extends Val {
  private final Object val;
  public WrapperVal(T val) { this.val = val; }
  // NOTE: I'm pretty confident here.
  // The check should be done at the one and only constructor.
  // If it doesn't, it means that whoever is using this class is using
  // generics is messing up on their end.
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
  public String toString() { return val.toString(); }
}
public final class Bool extends WrapperVal<Boolean> {
  public Bool(Boolean val) { super(val); }
  public final Map getMetaMap() { return MM_BOOL; }
  public final boolean truthy() { return getVal(); }
}
public final class Num extends WrapperVal<Double> {
  public Num(Double val) { super(val); }
  public final Map getMetaMap() { return MM_NUM; }
}
public final class Str extends WrapperVal<String> {
  public Str(String val) { super(val); }
  public final Map getMetaMap() { return MM_STR; }
}
public final class List extends WrapperVal<ArrayList<Val>> {
  public List(ArrayList<Val> val) { super(val); }
  public final Map getMetaMap() { return MM_LIST; }
}
public final class Map extends WrapperVal<HashMap<Val, Val>> {
  public Map() { super(new HashMap<Val, Val>()); }
  public Map(HashMap<Val, Val> val) { super(val); }
  public final Map getMetaMap() { return MM_MAP; }
  public Map put(BuiltinFunc bf) {
    getVal().put(new Str(bf.name), bf);
    return this;
  }
}
public abstract class Func extends Val {
  public abstract Val call(Val self, ArrayList<Val> args);
  public final Map getMetaMap() { return MM_FUNC; }
  public final boolean equals(Val other) { return this == other; }
}
public final class BoundFunc extends Func {
  public final Func f;
  public final Val self;
  public BoundFunc(Func f, Val self) { this.f = f; this.self = self; }
  public final Val call(Val self, ArrayList<Val> args) {
    return f.call(this.self, args);
  }
}
public abstract class BuiltinFunc extends Func {
  public final String name;
  public BuiltinFunc(String name) { this.name = name; }
  public final Val call(Val self, ArrayList<Val> args) {
    Simple.this.push(new BuiltinFuncFrame(name));
    try {
      return calli(self, args);
    } finally {
      Simple.this.pop();
    }
  }
  public abstract Val calli(Val self, ArrayList<Val> args);
}
public final class UserFunc extends Func {
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
    Simple.this.push(new UserFuncFrame(scope, token));
    try {
      Simple.this.put("self", self);
      if (vararg == null) {
        if (args.size() != this.args.size()) {
          throw new Err(
              "Expected " + Integer.toString(this.args.size()) +
              " arguments but got " + Integer.toString(args.size()));
        }
      } else {
        if (args.size() < this.args.size()) {
          throw new Err(
              "Expected at least " + Integer.toString(this.args.size()) +
              " arguments but got " + Integer.toString(args.size()));
        }
      }
      for (int i = 0; i < this.args.size(); i++)
        Simple.this.put(this.args.get(i), args.get(i));
      if (vararg != null) {
        ArrayList<Val> al = new ArrayList<Val>();
        for (int i = this.args.size(); i < args.size(); i++)
          al.add(args.get(i));
        Simple.this.put(vararg, new List(al));
      }

      return body.eval();
    } finally {
      Simple.this.pop();
    }
  }
}
public final class UserVal extends Val {
  private final Map metaMap;
  private final HashMap<String, Val> attrs;
  public UserVal(Map metaMap) { this(metaMap, new HashMap<String, Val>()); }
  public UserVal(Map metaMap, HashMap<String, Val> attrs) {
    this.metaMap = metaMap;
    this.attrs = attrs;
  }
  public final Map getMetaMap() { return metaMap; }
  // TODO: Make 'equals' overridable by user.
  public final boolean equals(Val other) { return this == other; }
  // TODO: Make 'hashCode' overridable by user.
  public final int hashCode() { return super.hashCode(); }
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
    super(message + Simple.this.getLocationString());
    this.message = message;
  }
}

//// Lexer

public static final class Lexer {
  public static final ArrayList<String> KEYWORDS = toArrayList(
      "and", "or", "xor", "return", "is", "import", "super",
      "def", "class", "not");
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
        "file '" + lexer.filespec + "' on line " + Integer.toString(lc) +
        "\n" + lexer.string.substring(a, b) + "\n" +
        spaces + "*";
  }
}

/// Parser

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
          }
        }

        if (at("=")) {
          token = next();
          if (vararg != null || args.size() != 1)
            throw new SyntaxError(
                token, "For setitem syntax, must have exactly one argument");
          node = new OperationAst(
                token, node, "__setitem__", parseExpression());
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
          node = new OperationAst(
              token, node, "__setattr__", new StringAst(token, name), value);
        } else {
          node = new OperationAst(
              token, node, "__getattr__", new StringAst(token, name));
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

    if (at("\\")) {
      Token token = next();
      ArrayList<String> args = new ArrayList<String>();
      String vararg = null;
      while (at("ID"))
        args.add((String) expect("ID").value);
      if (consume("*"))
        vararg = (String) expect("ID").value;
      expect(".");
      Ast body = parseExpression();
      return new FunctionAst(token, args, vararg, body);
    }

    throw new SyntaxError(peek(), "Expected expression");
  }
}

//// Ast
public abstract class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }
  public abstract Val eval();
}
public final class NumAst extends Ast {
  public final Num val;
  public NumAst(Token token, Double val) {
    super(token);
    this.val = new Num(val);
  }
  public final Val eval() { return val; }
}
public final class StringAst extends Ast {
  public final Str val;
  public StringAst(Token token, String val) {
    super(token);
    this.val = new Str(val);
  }
  public final Val eval() { return val; }
}
public final class NameAst extends Ast {
  public final String name;
  public NameAst(Token token, String name) {
    super(token);
    this.name = name;
  }
  public final Val eval() { return Simple.this.get(name); }
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
    if (jmp())
      return v;
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
    return new UserFunc(token, args, vararg, body, getFrame().scope);
  }
}
public final class ReturnAst extends Ast {
  public final Ast val;
  public ReturnAst(Token token, Ast val) {
    super(token);
    this.val = val;
  }
  public final Val eval() {
    Val v = val.eval();
    FLAG_RETURN = true;
    return v;
  }
}
public final class CallAst extends Ast {
  public final Ast f;
  public final ArrayList<Ast> args;
  public final Ast vararg;
  public CallAst(Token token, Ast f, ArrayList<Ast> args, Ast vararg) {
    super(token);
    this.f = f;
    this.args = args;
    this.vararg = vararg;
  }
  public final Val eval() {
    Val vf = f.eval();
    if (jmp())
      return vf;
    if (!(vf instanceof Func))
      throw new Err(
          "Expected Func but found " + vf.getClass().getName());
    ArrayList<Val> va = new ArrayList<Val>();
    for (int i = 0; i < args.size(); i++) {
      Val v = args.get(i).eval();
      if (jmp())
        return v;
      va.add(v);
    }
    if (vararg != null) {
      Val v = vararg.eval();
      if (!(v instanceof List))
        throw new Err(
            "Expected List for vararg but found " + v.getClass().getName());
      ArrayList<Val> vl = ((List) v).getVal();
      for (int i = 0; i < vl.size(); i++)
        va.add(vl.get(i));
    }
    return ((Func) vf).call(null, va);
  }
}
public final class GetMethodAst extends Ast {
  public final Ast owner;
  public final String name;
  public final Str nameStr;
  public GetMethodAst(Token token, Ast owner, String name) {
    super(token);
    this.owner = owner;
    this.name = name;
    nameStr = new Str(name);
  }
  public final Val eval() {
    Val v = owner.eval();
    if (jmp())
      return v;
    Val f = v.getMetaMap().getVal().get(nameStr);
    if (f == null || !(f instanceof Func))
      throw new Err("No method named " + name);
    return new BoundFunc((Func) f, v);
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
    Val l = left.eval();
    if (jmp()) return l;
    Val r = right.eval();
    if (jmp()) return r;
    return l == r ? tru : fal;
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
    Val l = left.eval();
    if (jmp()) return l;
    Val r = right.eval();
    if (jmp()) return r;
    return l != r ? tru : fal;
  }
}
public final class OperationAst extends Ast {
  public final Ast owner;
  public final String name;
  public final ArrayList<Ast> args;
  public final Str nameStr;
  public OperationAst(Token token, Ast owner, String name, Ast... args) {
    super(token);
    this.owner = owner;
    this.name = name;
    this.args = toArrayList(args);
    nameStr = new Str(name);
  }
  public final Val eval() {
    Val v = owner.eval();
    if (jmp()) return v;
    ArrayList<Val> al = new ArrayList<Val>();
    for (int i = 0; i < args.size(); i++) {
      Val vv = args.get(i).eval();
      if (jmp()) return vv;
      al.add(vv);
    }
    Simple.this.push(new AstFrame(token));
    try {
      return v.callMethod(nameStr, al);
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
    Val v = target.eval();
    if (jmp()) return v;
    return v.truthy() ? fal : tru;
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
    if (jmp()) return l;
    if (!l.truthy())
      return l;
    return right.eval();
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
    if (jmp()) return l;
    if (l.truthy())
      return l;
    return right.eval();
  }
}
public final class BlockAst extends Ast {
  public final ArrayList<Ast> body;
  public BlockAst(Token token, ArrayList<Ast> body) {
    super(token);
    this.body = body;
  }
  public final Val eval() {
    Val v = nil;
    for (int i = 0; i < body.size(); i++) {
      v = body.get(i).eval();
      if (jmp()) return v;
    }
    return v;
  }
}
public final class ModuleAst extends Ast {
  public final String name;
  public final BlockAst body;
  public ModuleAst(Token token, String name, BlockAst body) {
    super(token);
    this.name = name;
    this.body = body;
  }
  public final Val eval() {
    return body.eval();
  }
}
//// Utils
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
  public Scope put(BuiltinFunc bf) {
    return put(bf.name, bf);
  }
}

}
