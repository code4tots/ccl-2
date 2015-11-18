import java.util.ArrayList;
import java.util.HashMap;

public class Blarg {

  public static class Token {
    public final String source;
    public final int position;
    public final String type;
    public final String text;
    public final Double doubleValue;
    public final String stringValue;

    public Token(String source, int position, String type, String text, Double doubleValue, String stringValue) {
      this.source = source;
      this.position = position;
      this.type = type;
      this.text = text;
      this.doubleValue = doubleValue;
      this.stringValue = stringValue;
    }
  }

  public static class TokenStream {
    public final String string;
    public int start, end;
    public Token lookahead;
    public boolean finished;

    public TokenStream(String s) {
      string = s;
      start = end = 0;
      finished = false;
      lookahead = extractNext();
    }

    public boolean at(String type) {
      return lookahead.type == type;
    }

    public boolean consume(String type) {
      if (at(type)) {
        next();
        return true;
      }
      return false;
    }

    public Token expect(String type) {
      if (!at(type))
        throw new RuntimeException("Expected '" + type + "' but found '" + lookahead.type + "'");
      return next();
    }

    public Token next() {
      Token token = lookahead;
      lookahead = extractNext();
      return token;
    }

    public void skipSpacesAndComments() {
      while (true) {
        while (end < string.length() && Character.isWhitespace(string.charAt(end)))
          end++;

        if (end < string.length() && string.charAt(end) == ';')
          while (end < string.length() && string.charAt(end) != '\n')
            end++;
        else
          break;
      }
      start = end;
    }

    public String sliceCurrentText() {
      return string.substring(start, end);
    }

    public Token makeToken(String type) {
      Token token = new Token(string, start, type, sliceCurrentText(), null, null);
      start = end;
      return token;
    }

    public Token makeNumberToken(Double value) {
      Token token = new Token(string, start, "NUM", sliceCurrentText(), value, null);
      start = end;
      return token;
    }

    public Token makeStringToken(String value) {
      Token token = new Token(string, start, "STR", sliceCurrentText(), null, value);
      start = end;
      return token;
    }

    public static boolean isSymbolCharacter(char c) {
      return Character.isAlphabetic(c) ||
          c == '%' || c == '*' || c == '+' || c == '-' ||
          c == '1' || c == '2' ||
          c == '<' || c == '=' || c == '>' ||
          c == '&';
    }

    public Token extractNext() {
      if (finished)
        throw new RuntimeException("No more tokens");

      skipSpacesAndComments();

      if (end >= string.length()) {
        finished = true;
        return makeToken("EOF");
      }

      if (string.charAt(end) == '\'') {
        end++;
        return makeToken("'");
      }

      if (string.charAt(end) == '(') {
        end++;
        return makeToken("(");
      }

      if (string.charAt(end) == ')') {
        end++;
        return makeToken(")");
      }

      char c = string.charAt(end);
      if (c == '+' || c == '-' || Character.isDigit(c) || c == '.') {
        if (c == '+' || c == '-')
          end++;

        boolean digitSeen = false;

        while (end < string.length() && Character.isDigit(string.charAt(end))) {
          digitSeen = true;
          end++;
        }

        if (string.charAt(end) == '.')
          end++;

        while (end < string.length() && Character.isDigit(string.charAt(end))) {
          digitSeen = true;
          end++;
        }

        if (digitSeen)
          return makeNumberToken(Double.valueOf(sliceCurrentText()));
        else
          end = start;
      }

      if (c == '"') {
        end++;
        StringBuilder sb = new StringBuilder();
        while (end < string.length() && string.charAt(end) != '"') {
          if (string.charAt(end) == '\\') {
            end++;
            switch(string.charAt(end)) {
            case '\\':
              sb.append('\\');
              break;
            case 'n':
              sb.append('\n');
              break;
            case 't':
              sb.append('\t');
              break;
            default:
              throw new RuntimeException("Unrecognized string escape '" + string.substring(end-1, end+1) + "'");
            }
            end++;
          } else
            sb.append(string.charAt(end++));
        }
        if (end < string.length())
          end++;
        return makeStringToken(sb.toString());
      }

      while (end < string.length() && isSymbolCharacter(string.charAt(end)))
        end++;

      if (start != end)
        return makeToken("SYM");

      while (end < string.length() && !Character.isWhitespace(string.charAt(end)))
        end++;

      throw new RuntimeException("Unrecognized token '" + string.substring(start, end) + "'");
    }
  }

  abstract public static class Thing {
    abstract public String type();
    public String symbolValue() {
      throw new RuntimeException("symbolValue not implemented");
    }
    public String textValue() {
      throw new RuntimeException("textValue not implemented");
    }
    public Double numberValue() {
      throw new RuntimeException("numberValue not implemented");
    }
    public ArrayList<Thing> listValue() {
      throw new RuntimeException("listValue not implemented");
    }
    public HashMap<Thing, Thing> mapValue() {
      throw new RuntimeException("mapValue not implemented");
    }
    public boolean equals(Object thing) {
      return (thing instanceof Thing) && equals((Thing) thing);
    }
    abstract public boolean equals(Thing thing);
    public int hashCode() { return 0; }
  }
  abstract public static class Atom extends Thing {}
  final public static class Nil extends Atom {
    public String type() { return "Nil"; }
    public boolean equals(Thing thing) { return thing instanceof Nil; }
  }
  final public static Nil nil = new Nil();
  final public static class Symbol extends Atom {
    public String type() { return "Symbol"; }
    public String value;
    public Symbol(String value) { this.value = value; }
    public boolean equals(Thing thing) {
      return type().equals(thing.type()) && value.equals(thing.symbolValue());
    }
    public String symbolValue() {
      return value;
    }
    public int hashCode() { return value.hashCode(); }
    public String toString() {
      return value;
    }
  }
  final public static class Text extends Atom {
    public String type() { return "Text"; }
    public String value;
    public Text(String value) { this.value = value; }
    public boolean equals(Thing thing) {
      return type().equals(thing.type()) && value.equals(thing.symbolValue());
    }
    public String textValue() {
      return value;
    }
    public int hashCode() { return value.hashCode(); }
  }
  final public static class Number extends Atom {
    public String type() { return "Number"; }
    public Double value;
    public Number(Double value) { this.value = value; }
    public boolean equals(Thing thing) {
      return type().equals(thing.type()) && value.equals(thing.numberValue());
    }
    public Double numberValue() {
      return value;
    }
    public int hashCode() { return value.hashCode(); }
  }
  final public static class List extends Thing {
    public String type() { return "List"; }
    public ArrayList<Thing> items = new ArrayList<Thing>();
    public List(Thing... args) {
      for (int i = 0; i < args.length; i++)
        items.add(args[i]);
    }
    public boolean equals(Thing thing) {
      return type().equals(thing.type()) && items.equals(thing.listValue());
    }
    public ArrayList<Thing> listValue() {
      return items;
    }
    public int hashCode() { return items.hashCode(); }
    public String toString() {
      StringBuilder sb = new StringBuilder("(");
      for (int i = 0; i < items.size(); i++) {
        if (i != 0)
          sb.append(" ");
        sb.append(items.get(i));
      }
      sb.append(")");
      return sb.toString();
    }
  }
  final public static class Map extends Thing {
    public String type() { return "Map"; }
    public HashMap<Thing, Thing> map = new HashMap<Thing, Thing>();
    public Map(Thing... args) {
      for (int i = 0; i < args.length; i += 2)
        map.put(args[i], args[i+1]);
    }
    public boolean equals(Thing thing) {
      return type().equals(thing.type()) && map.equals(thing.mapValue());
    }
    public HashMap<Thing, Thing> mapValue() {
      return map;
    }
    public int hashCode() { return map.hashCode(); }
  }
  final public static class Lambda extends Thing {
    public String type() { return "Lambda"; }
    final public Thing context;
    final public ArrayList<String> args;
    final public String rest;
    final public ArrayList<Thing> body;
    public Lambda(Thing context, ArrayList<String> args, String rest, ArrayList<Thing> body) {
      this.context = context;
      this.args = args;
      this.rest = rest;
      this.body = body;
    }
    public boolean equals(Thing thing) {
      return this == thing;
    }
  }

  // reader
  public static List parse(String text) {
    return parse(new TokenStream(text));
  }

  public static List parse(TokenStream stream) {
    List toplevel = new List(new Symbol("do"));
    while (!stream.finished)
      toplevel.listValue().add(parseOne(stream));
    return toplevel;
  }

  public static Thing parseOne(TokenStream stream) {
    if (stream.consume("'"))
      return new List(new Symbol("quote"), parseOne(stream));
    else if (stream.at("("))
      return parseList(stream);
    else if (stream.at("NUM"))
      return new Number(stream.next().doubleValue);
    else if (stream.at("SYM"))
      return new Symbol(stream.next().text);
    else if (stream.at("STR"))
      return new Text(stream.next().stringValue);
    else
      throw new RuntimeException("Unexpected token " + stream.lookahead.type);
  }

  public static List parseList(TokenStream stream) {
    List list = new List();
    stream.expect("(");
    while (!stream.consume(")"))
      list.listValue().add(parseOne(stream));
    return list;
  }

  public static class Evaluator {
  }

  // Run with '-ea' flag. e.g. javac Blarg.java && java -ea Blarg
  public static void test() {
    // tokenizer test.
    TokenStream ts = new TokenStream("(hello world 1 2 3) 5.4 \"hi\"");
    Token tok;
    tok = ts.next();
    assert tok.type == "(": tok.type;
    tok = ts.next();
    assert tok.type == "SYM": tok.type;
    assert tok.text.equals("hello"): tok.text;
    tok = ts.next();
    assert tok.type == "SYM": tok.type;
    assert tok.text.equals("world"): tok.text;
    tok = ts.next();
    assert tok.type == "NUM": tok.type;
    assert tok.doubleValue == 1.0;
    tok = ts.next();
    assert tok.type == "NUM": tok.type;
    assert tok.doubleValue == 2.0;
    tok = ts.next();
    assert tok.type == "NUM": tok.type;
    assert tok.doubleValue == 3.0;
    tok = ts.next();
    assert tok.type == ")": tok.type;
    tok = ts.next();
    assert tok.type == "NUM": tok.type;
    assert tok.doubleValue == 5.4;
    tok = ts.next();
    assert tok.type == "STR": tok.type;
    assert tok.stringValue.equals("hi"): tok.stringValue;
    assert ts.finished;

    // Thing test
    assert new Symbol("hi").equals(new Symbol("hi"));
    assert new Number(5.5).equals(new Number(5.5));
    assert new List(new Symbol("hi")).equals(new List(new Symbol("hi")));
    HashMap<Thing, Thing> m1 = new HashMap<Thing, Thing>(), m2 = new HashMap<Thing, Thing>();
    m1.put(new Symbol("hi"), new Number(4.23));
    m2.put(new Symbol("hi"), new Number(4.23));
    assert m1.equals(m2);
    assert new Map(new Symbol("a"), new Symbol("b")).equals(new Map(new Symbol("a"), new Symbol("b")));
    assert new List(new Symbol("hi"), new Symbol("there")).toString().equals("(hi there)");

    // parse test
    assert parse("").equals(new List(new Symbol("do"))): parse("");
    assert parse("(print 5)").equals(new List(new Symbol("do"), new List(new Symbol("print"), new Number(5.0)))): parse("(print 5)");
  }

  public static void main(String[] args) {
    test();
  }
}
