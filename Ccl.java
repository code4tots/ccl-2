// javac Ccl.java -Xlint
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
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

    throw err("Tried to eval node of type: " + node.getClass().getName());
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

  public static Object call(String name, Object... args) {
    Func f = (Func) ROOT_CONTEXT.get(name);
    return f.invoke(new ArrayList(Arrays.asList(args)));
  }

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
  abstract public static class Builtin {
    public final String name;
    public Builtin(String name) { this.name = name; }
    public void expectArgumentLength(int expected, int actual) {
      if (expected != actual)
        throw err(name + " expected " + Integer.toString(expected) + " arguments, but found " + Integer.toString(actual));
    }
  }
  abstract public static class Macro extends Builtin {
    public Macro(String name) { super(name); }
    abstract public Object invoke(HashMap context, ArrayList args);
    public String toString() { return "<macro " + name + ">"; }
  }

  abstract public static class Func extends Builtin {
    public Func(String name) { super(name); }
    abstract public Object invoke(ArrayList args);
    public String toString() { return "<func " + name + ">"; }
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
        items.add(parseOne());
        skipSpaces();
      }
      return items;
    }

    public ArrayList parseMany() {
      ArrayList items = new ArrayList();
      skipSpaces();
      while (s.charAt(i) != ')') {
        items.add(parseOne());
        skipSpaces();
      }
      return items;
    }

    private Object parseOne() {
      char c = s.charAt(i);
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
      while (i < s.length() && (Character.isDigit(s.charAt(i)) ||
                                s.charAt(i) == '.' ||
                                s.charAt(i) == '-'))
        i++;
      return Double.parseDouble(s.substring(j, i));
    }

    private ArrayList parseStringLiteral() {
      StringBuilder sb = new StringBuilder();
      boolean raw = s.charAt(i) == 'r';
      if (raw)
        i++;
      String quote = (s.startsWith("'''", i) || s.startsWith("\"\"\"", i)) ? s.substring(i, i+3) : s.substring(i, i+1);
      i += quote.length();
      while (!s.startsWith(quote, i)) {
        if (i >= s.length())
          throw err("You need end quotes!");
        char c = s.charAt(i);
        if (!raw && c == '\\') {
          i++;
          if (i >= s.length())
            throw err("You tried to escape the string, but the file ended...");
          c = s.charAt(i);
          switch(c) {
          case 'n': sb.append('\n');
          default: throw err("Unrecognized escape: " + Character.toString(c));
          }
        }
        else {
          sb.append(c);
          i++;
        }
      }
      i += quote.length();
      ArrayList lit = new ArrayList();
      lit.add("quote");
      lit.add(sb.toString());
      return lit;
    }

    private ArrayList parseForm() {
      i++; // consume '('
      ArrayList items = parseMany();
      i++; // consume ')'
      return items;
    }

    private String parseName() {
      int j = i;
      while (i < s.length() && !Character.isWhitespace(s.charAt(i)) && s.charAt(i) != ')')
        i++;
      return s.substring(j, i);
    }

    private void skipSpaces() {
      while (i < s.length() && (Character.isWhitespace(s.charAt(i))))
        i++;
    }
  }

  //*** Root context

  public static final HashMap ROOT_CONTEXT = new HashMap();
  static {
    ROOT_CONTEXT.put("block", new Macro("block") {
      public Object invoke(HashMap context, ArrayList args) {
        Object last = null;
        for (int i = 0; i < args.size(); i++)
          last = eval(context, args.get(i));
        return last;
      }
    });

    ROOT_CONTEXT.put("print", new Func("print") {
      public Object invoke(ArrayList args) {
        for (int i = 0; i < args.size(); i++) {
          if (i > 0)
            System.out.print(" ");
          System.out.print(args.get(i));
        }
        System.out.println();
        return args.size() > 0 ? args.get(0) : null;
      }
    });

    ROOT_CONTEXT.put("quote", new Macro("quote") {
      public Object invoke(HashMap context, ArrayList args) {
        expectArgumentLength(1, args.size());
        return args.get(0);
      }
    });

    ROOT_CONTEXT.put("type-of", new Func("type-of") {
      public Object invoke(ArrayList args) {
        expectArgumentLength(1, args.size());
        Object val = args.get(0);
        if (val == null)
          return "nil";
        if (val instanceof Double)
          return "num";
        if (val instanceof String)
          return "str";
        if (val instanceof ArrayList)
          return "list";
        if (val instanceof HashMap)
          return "dict";
        throw err("Unrecognized type: " + val.getClass().getName());
      }
    });

    ROOT_CONTEXT.put("add", new Func("add") {
      public Object invoke(ArrayList args) {
        double total = 0;
        for (int i = 0; i < args.size(); i++) {
          if (!(args.get(i) instanceof Double))
            throw err("add expects only num arguments but found: " + call("type-of", args.get(i)).toString());
          total += (Double) args.get(i);
        }
        return total;
      }
    });

    ROOT_CONTEXT.put("join", new Func("join") {
      public Object invoke(ArrayList args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
          if (!(args.get(i) instanceof String))
            throw err("join expects only str arguments but found: " + call("type-of", args.get(i)).toString());
          sb.append(args.get(i).toString());
        }
        return total;
      }
    });
  }

  //*** main

  public static String slurpInputStream(InputStream stream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null)
      sb.append(line).append("\n");
    reader.close();
    return sb.toString();
  }

  public static String slurpFile(String path) throws IOException {
    FileInputStream fin = new FileInputStream(new File(path));
    String contents = slurpInputStream(fin);
    fin.close();
    return contents;
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1)
      throw err("Expected 1 argument but found: " + Arrays.asList(args).toString());
    String code = slurpFile(args[0]);
    eval(ROOT_CONTEXT, parse(code));
  }
}
