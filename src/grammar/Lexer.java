import java.util.ArrayList;
import java.math.BigInteger;

public final class Lexer {
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
