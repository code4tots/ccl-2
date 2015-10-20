// javac Ccl.java -Xlint
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Ccl {

  //*** Star of the show, eval!
  public static Object eval(HashMap context, Object node) {
    if (node instanceof Double)
      return node;

    if (node instanceof String)
      return lookup(context, (String) node);

    if (node instanceof ArrayList)
      return invoke(context, (ArrayList) node);

    throw err("Tried to eval node of type: " + node.getClass().toString());
  }

  //*** Functions for manipulating context.
  public static HashMap find(HashMap context, String key) {
    if (context.containsKey(key))
      return context;

    if (context.containsKey("__parent__")) {
      Object parent = context.get("__parent__");
      if (!(parent instanceof HashMap))
        throw err("Expected __parent__ to be of type HashMap but found '" + parent.getClass().toString() + "'");
      return find((HashMap) parent, key);
    }

    return null;
  }

  public static Object lookup(HashMap context, String key) {
    HashMap c = find(context, key);
    if (c == null)
      throw err("Name '" + key + "' not found");
    return c.get(key);
  }

  //**** Invoke.
  public static Object invoke(HashMap context, ArrayList items) {
    if (items.size() == 0)
      throw err("Tried to eval-invoke an empty list");

    Object f = eval(context, items.get(0));
    ArrayList args = new ArrayList();

    for (int i = 1; i < items.size(); i++)
      args.add(items.get(i));

    if (f instanceof Macro)
      return ((Macro)f).invoke(context, args);

    if (f instanceof Func) {
      for (int i = 0; i < args.size(); i++)
        args.set(i, eval(context, args.get(i)));
      return ((Func)f).invoke(args);
    }

    throw err("Tried to invoke an object of type: " + f.getClass().toString());
  }

  //*** error handling place holder
  public static RuntimeException err(String message) {
    return new RuntimeException(message);
  }

  //*** For stuff where builtin types couldn't handle it.
  abstract public static class Macro {
    public final String name;
    public Macro(String name) { this.name = name; }
    abstract public Object invoke(HashMap context, ArrayList args);
  }

  abstract public static class Func {
    public final String name;
    public Func(String name) { this.name = name; }
    abstract public Object invoke(ArrayList args);
  }

  //*** parse
  public static ArrayList parse(String s) {
    return new Parser(s).parse();
  }

  private static class Parser {
    public final String s;
    public int i;

    public Parser(String s) {
      this.s = s;
      i = 0;
    }

    public ArrayList parse() {
      ArrayList items = new ArrayList();
      items.add("block");
      skipSpaces();
      while (i < s.length()) {
        items.add(parseOne())
        skipSpaces();
      }
      return items;
    }

    public ArrayList parseMany() {
      ArrayList items = new ArrayList();
      skipSpaces();
      while (s.get(i) != ')') {
        items.add(parseOne())
        skipSpaces();
      }
      return items;
    }

    private Object parseOne() {
      char c = s.get(i);
      if (c == '-' || c == '.' || Character.isDigit(c))
        return parseNumber();

      if (c == '"' || c == '\'' || s.startsWith("r'", i) || s.startsWith("r\"", i))
        return parseStringLiteral();

      if (c == '(')
        return parseForm();

      return parseName();
    }

    private Double parseNumber() {
      int j = i;
      while (i < s.length() && s.get(i) != ')')
        i++;
    }

    private ArrayList parseStringLiteral() {
      StringBuilder sb = new StringBuilder();

      ArrayList lit = new ArrayList();
      lit.add("quote");
      lit.add(sb.toString());
      return lit;
    }

    private void skipSpaces() {
      while (i < s.length() && (Character.isWhitespaceChar(s.get(i))))
        i++;
    }
  }

  //*** main
  public static void main(String[] args) {

    new ArrayList(Arrays.asList(1, 2, 3));
  }
}
