/* A simple programming language.
 */

import java.util.ArrayList;
import java.util.HashMap;

public class Simple {

  /*-- Sanity checks --*/

  // Requires assertions to be turned on '-ea' flag when running
  // the 'java' (not when compiling with javac).
  public static void runSanityChecks() {

    // Note: Lexer always appends an extra newline before EOF.
    ArrayList<Token> tokens = lex("a 'b' .3\n()", "<test>");
    assert tokens.get(0).type.equals("ID"): tokens.get(0).type;
    assert tokens.get(0).value.equals("a"): tokens.get(0).value;
    assert tokens.get(1).type.equals("STR"): tokens.get(1).type;
    assert tokens.get(1).value.equals("b"): tokens.get(1).value;
    assert tokens.get(2).type.equals("NUM"): tokens.get(2).type;
    assert tokens.get(2).value.equals(".3"): tokens.get(2).value;
    assert tokens.get(3).type.equals("\n"): tokens.get(3).type;
    assert tokens.get(4).type.equals("("): tokens.get(4).type;
    assert tokens.get(5).type.equals(")"): tokens.get(5).type;
    assert tokens.get(6).type.equals("\n"): tokens.get(6).type;
    assert tokens.get(7).type.equals("EOF"): tokens.get(7).type;
    assert tokens.size() == 8;
    System.out.println("* lexer sanity checks done *");

    System.out.println("*** All sanity checks look good! ***");
  }

  public static void main(String[] args) {
    runSanityChecks();
  }

  /*-- Err --*/
  public static final class Err extends RuntimeException {
    private final ArrayList<Traceable> trace = new ArrayList<Traceable>();

    public Err(String message) {
      super(message);
    }

    public void addTraceable(Traceable traceable) {
      trace.add(traceable);
    }
  }

  public static interface Traceable {
    public String getTraceMessage();
  }

  /*-- Lexer --*/
  public static ArrayList<Token> lex(String text, String filespec) {
    int i = 0;
    final Source source = new Source(text, filespec);
    final ArrayList<Token> tokens = new ArrayList<Token>();
    while (true) {
      while (i < text.length() &&
          Character.isWhitespace(text.charAt(i)) &&
          text.charAt(i) != '\n')
        i++;

      if (i >= text.length())
        break;

      if (text.charAt(i) == '\n' ||
          text.charAt(i) == '(' ||
          text.charAt(i) == ')' ||
          text.charAt(i) == '{' ||
          text.charAt(i) == '}') {
        tokens.add(new Token(
            source, Character.toString(text.charAt(i)), i, null));
        i++;
        continue;
      }

      final int j = i;

      if (text.charAt(i) == '"' || text.charAt(i) == '\'' ||
          text.startsWith("r'", i) || text.startsWith("r\"")) {
        boolean raw = false;
        if (text.charAt(i) == 'r') {
          raw = true;
          i++;
        }
        String quotes = text.substring(i, i+1);
        if (text.startsWith("'''", i) || text.startsWith("\"\"\"", i))
          quotes = text.substring(i, i+3);
        i += quotes.length();
        StringBuilder sb = new StringBuilder();
        while (i < text.length() && !text.startsWith(quotes, i)) {
          if (!raw && text.charAt(i) == '\\') {
            i++;
            if (i >= text.length())
              throw new Err("String escape in string literal, but found EOF");
            switch (text.charAt(i)) {
            case 'n':
              sb.append('\n');
              break;
            case 't':
              sb.append('\t');
              break;
            default:
              throw new Err("Unrecognized string escape: " + text.charAt(i));
            }
            i++;
          } else {
            sb.append(text.charAt(i));
            i++;
          }
        }
        if (i >= text.length())
          throw new Err("Unterminated string literal");
        i += quotes.length();
        tokens.add(new Token(source, "STR", j, sb.toString()));
        continue;
      }

      while (text.charAt(i) == '_' || Character.isLetter(text.charAt(i)))
        i++;

      if (j != i) {
        tokens.add(new Token(source, "ID", j, text.substring(j, i)));
        continue;
      }

      if (Character.isDigit(text.charAt(i)) ||
          (i+1 < text.length() &&
              (text.charAt(i) == '.' || text.charAt(i) == '-') &&
              Character.isDigit(text.charAt(i+1))) ||
          (i+2 < text.length() &&
              text.charAt(i) == '-' &&
              text.charAt(i+1) == '.' &&
              Character.isDigit(text.charAt(i+2)))) {
        if (text.charAt(i) == '-')
          i++;
        while (Character.isDigit(text.charAt(i)))
          i++;
        if (text.charAt(i) == '.')
          i++;
        while (Character.isDigit(text.charAt(i)))
          i++;
        tokens.add(new Token(source, "NUM", j, text.substring(j, i)));
        continue;
      }

      throw new Err("Invalid token: " + text.substring(i, i+6));
    }
    tokens.add(new Token(source, "\n", i, null));
    tokens.add(new Token(source, "EOF", i, null));
    return tokens;
  }

  public static final class Source {
    public final String text;
    public final String filespec;

    public Source(String text, String filespec) {
      this.text = text;
      this.filespec = filespec;
    }
  }

  public static final class Token {
    public final Source source;
    public final String type;
    public final int i;
    public final String value;

    public Token(Source source, String type, int i, String value) {
      this.source = source;
      this.type = type;
      this.i = i;
      this.value = value;
    }

    public String getLocationMessage() {
      return "on line " + getLineNumber() + " in '" + source.filespec + "'";
    }

    public int getLineNumber() {
      int ln = 1;
      for (int i = 0; i < this.i; i++)
        if (source.text.charAt(i) == '\n')
          ln++;
      return ln;
    }
  }

  /*-- Parser --*/
  public static final class Parser {
    private int i;
  }

  /*-- AST --*/
  public abstract static class Ast {
    public final Token token;

    public Ast(Token token) {
      this.token = token;
    }

    public abstract Value eval(Scope scope);
  }

  public static final class Command extends Ast {
    public final Ast f;
    public final ArrayList<Ast> args;

    public Command(Token token, Ast f, ArrayList<Ast> args) {
      super(token);
      this.f = f;
      this.args = args;
    }

    public Value eval(Scope scope) {
      Callable callable = (Callable) f.eval(scope);
      return callable.call(scope, args);
    }
  }

  public static final class Name extends Ast {
    public final String name;
    public Name(Token token, String name) {
      super(token);
      this.name = name;
    }
    public Value eval(Scope scope) {
      return scope.get(name);
    }
  }

  public static final class Literal extends Ast {
    public final Value value;
    public Literal(Token token, Value value) {
      super(token);
      this.value = value;
    }
    public Value eval(Scope scope) {
      return value;
    }
  }

  /*-- Value --*/
  public abstract static class Value {}

  public static final class Number extends Value {
    public static final Number ZERO = new Number(0.0);

    public final Double value;

    public Number(Double value) { this.value = value; }
  }

  public static final class Text extends Value {
    public final String value;
    public Text(String value) { this.value = value; }
  }

  public static final class List extends Value {
    public final ArrayList<Value> value;
    public List(ArrayList<Value> value) { this.value = value; }
  }

  public static final class Table extends Value {
    public final HashMap<Value, Value> value;
    public Table(HashMap<Value, Value> value) { this.value = value; }
  }

  public abstract static class Callable extends Value {
    public abstract Value call(Scope scope, ArrayList<Ast> args);
  }

  public abstract static class SpecialForm extends Callable {}

  public abstract static class BaseFunction extends Callable {
    @Override /* Callable */
    public Value call(Scope scope, ArrayList<Ast> rawargs) {
      ArrayList<Value> args = new ArrayList<Value>();
      for (Ast ast: rawargs)
        args.add(ast.eval(scope));
      return call(args);
    }

    public abstract Value call(ArrayList<Value> args);
  }

  public static final class Function extends BaseFunction
      implements Traceable {
    public final Token token;
    public final String name; // nullable
    public final Scope scope;
    public final Ast body;
    public final ArrayList<String> argnames;

    public Function(Token token, String name, Scope scope, Ast body,
        ArrayList<String> argnames) {
      this.token = token;
      this.name = name;
      this.scope = scope;
      this.body = body;
      this.argnames = argnames;
    }

    @Override /* BaseFunction */
    public Value call(ArrayList<Value> args) {
      int i = 0;
      Scope scope = new Scope(this.scope);
      for (; i < argnames.size() && i < args.size(); i++)
        scope.put(argnames.get(i), args.get(i));
      for (; i < argnames.size(); i++)
        scope.put(argnames.get(i), Number.ZERO);
      return body.eval(scope);
    }

    public String getName() {
      return name == null ? "<anonymous>" : name;
    }

    @Override /* Traceable */
    public String getTraceMessage() {
      return "in function '" + getName() + "' " + token.getLocationMessage();
    }
  }

  public abstract static class BuiltinFunction extends BaseFunction
      implements Traceable {
    public abstract String getName();

    @Override /* Traceable */
    public String getTraceMessage() {
      return "in builtin function '" + getName() + "'";
    }
  }

  /*-- Scope --*/
  public static final class Scope {
    private final Scope parent;
    private final HashMap<String, Value> table = new HashMap<String, Value>();
    public Scope(Scope parent) {
      this.parent = parent;
    }

    public Scope put(String key, Value value) {
      table.put(key, value);
      return this;
    }

    public Value get(String key) {
      Value value = table.get(key);
      if (value == null) {
        if (parent == null) {
          throw new Err("Key '" + key + "' not found in scope");
        } else {
          return parent.get(key);
        }
      } else {
        return value;
      }
    }
  }
}
