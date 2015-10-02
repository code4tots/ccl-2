import java.util.ArrayList;
import java.util.HashMap;

// TODO: Better error messages all around.
// TOOD: Better error checking.
public class Simple {

  public static RuntimeException err(String message) {
    return new RuntimeException(message);
  }

  private static int getLineCount(String s, int i) {
    String sliced = s.substring(0, i);
    return sliced.length() - sliced.replace("\n", "").length() + 1;
  }

  public static boolean isWordCharacter(char c) {
    return Character.isAlphabetic(c) || Character.isDigit(c) ||
           c == '_' || c == '!' || c == '@' || c == '#' ||
           c == '%' || c == '^' || c == '&' || c == '*' ||
           c == '-' || c == '+' || c == '=';
  }

  public static Ast parse(String s) {
    int i = 0;
    ArrayList<ListAst> stack = new ArrayList<ListAst>();
    stack.add(new ListAst(s, 0));
    stack.get(stack.size()-1).add(new StrAst(s, 0, "begin"));
    while (true) {
      while (i < s.length() && Character.isWhitespace(s.charAt(i)))
        i++;

      if (i >= s.length())
        break;

      int j = i;
      char c = s.charAt(i);

      if (c == '(') {
        i++;
        stack.add(new ListAst(s, j));
        continue;
      }

      if (c == ')') {
        if (stack.size() == 1)
          throw err("Close parenthesis without matching open parenthesis on line: " + Integer.toString(getLineCount(s, i)));
        i++;
        ListAst list = stack.remove(stack.size() - 1);
        stack.get(stack.size() - 1).add(list);
        continue;
      }

      // String literal.
      {
        boolean raw = false;
        if (c == 'r') {
          raw = true;
          i++;
        }

        if (i < s.length() && (s.charAt(i) == '"' ||
                               s.charAt(i) == '\'')) {
          char qchar = s.charAt(i);
          String quote;
          if (i+2 < s.length() && s.charAt(i+1) == qchar && s.charAt(i+2) == qchar)
            quote = s.substring(i, i+2);
          else
            quote = Character.toString(qchar);

          i += quote.length();

          StringBuilder sb = new StringBuilder();

          while (i < s.length() && !s.startsWith(quote, i)) {
            char d = s.charAt(i);
            if (!raw && d == '\\') {
              if (i >= s.length())
                throw err("Last character of your program is \\ and your string is unterminated");
              i++;
              char e = s.charAt(i);
              switch (e) {
              case '\\': sb.append(d); break;
              case 'n': sb.append('\n'); break;
              default: throw err("Unrecognized string escape: " + Character.toString(e));
              }
            }
            else sb.append(d);
            i++;
          }

          if (i >= s.length())
            throw err("Unterminated quotes");

          if (!s.startsWith(quote, i))
            throw err("FUBAR");

          i += quote.length();

          ListAst item = new ListAst(s, j);
          item.add(new StrAst(s, j, "quote"));
          item.add(new StrAst(s, j, sb.toString()));
          stack.get(stack.size() - 1).add(item);
          continue;
        }
        i = j;
      }

      // Number
      {
        if (c == '-')
          i++;
        boolean seenDot = false;
        if (i < s.length() && s.charAt(i) == '.') {
          seenDot = true;
          i++;
        }
        if (i < s.length() && Character.isDigit(s.charAt(i))) {
          while (i < s.length() && Character.isDigit(s.charAt(i)))
            i++;
          if (!seenDot) {
            if (i < s.length() && s.charAt(i) == '.')
              i++;
            while (i < s.length() && Character.isDigit(s.charAt(i)))
              i++;
          }
          stack.get(stack.size() - 1).add(new NumAst(s, j, Double.parseDouble(s.substring(j, i))));
          continue;
        }
      }
      i = j;

      // Name
      while (i < s.length() && isWordCharacter(s.charAt(i)))
        i++;

      if (j < i) {
        stack.get(stack.size() - 1).add(new StrAst(s, j, s.substring(j, i)));
        continue;
      }

      // getattr
      if (c == '.') {
        i++;

        if (stack.get(stack.size()-1).size() == 0)
          throw err("Tried to use '.' operator, but there's nothing to the left of it: line " + Integer.toString(getLineCount(s, i)));

        ListAst current = stack.get(stack.size()-1);
        Ast owner = current.pop();

        int k = i;

        while (i < s.length() && isWordCharacter(s.charAt(i)))
          i++;

        if (k == i)
          throw err("Tried to use '.' operator, but there's no attribute name on the right side: line " + Integer.toString(getLineCount(s, i)));

        String attribute = s.substring(k, i);

        ListAst item = new ListAst(s, j);
        item.add(new StrAst(s, j, "."));
        item.add(owner);
        item.add(new StrAst(s, k, attribute));
        current.add(item);
        continue;
      }

      // TODO: Better error message.
      throw err("Invalid token on line " + Integer.toString(getLineCount(s, i)));
    }

    if (stack.size() > 1)
      throw err("Too many open parenthesis");

    return stack.get(0);
  }

  public static abstract class Context {
    public abstract Obj lookup(String name);
    public abstract void assign(String name, Obj value);
    public abstract void define(String name, Obj value);
    public Obj xlookup(Ast node, String name) {
      Obj value = lookup(name);
      if (value == null)
        throw err("Name not found: '" + name + "' see line " + Integer.toString(getLineCount(node.source, node.i)));
      return value;
    }
  }

  public static final class RootContext extends Context {
    private final HashMap<String, Obj> table = new HashMap<String, Obj>();
    public Obj lookup(String name) { return table.get(name); }
    public void assign(String name, Obj value) {
      if (!table.containsKey(name))
        throw err("Unrecognized name in assignment: " + name);
      table.put(name, value);
    }
    public void define(String name, Obj value) {
      table.put(name, value);
    }
  }

  public static final class ChildContext extends Context {
    private final HashMap<String, Obj> table = new HashMap<String, Obj>();
    private final Context parent;
    public ChildContext(Context parent) { this.parent = parent; }
    public Obj lookup(String name) {
      Obj value = table.get(name);
      return value == null ? parent.lookup(name) : value;
    }
    public void assign(String name, Obj value) {
      if (table.containsKey(name))
        table.put(name, value);
      else
        parent.assign(name, value);
    }
    public void define(String name, Obj value) {
      table.put(name, value);
    }
  }

  public static abstract class Obj {

    public Obj callForm(Context context, ArrayList<Ast> args) {
      throw err("Calling this object as a form is not supported");
    }

    public Obj call(Obj... args) {
      throw err("Calling this object as a function is not supported");
    }

    public Obj bind(Obj owner) {
      return this;
    }

    public abstract Dict getMetaDict();
  }

  public static class Num extends Obj {
    private final Double value;
    public Num(Double v) { value = v; }
    public final Dict getMetaDict() { return NUM_META_DICT; }
  }

  public static class Str extends Obj {
    private final String value;
    public Str(String v) { value = v; }
    public final Dict getMetaDict() { return STR_META_DICT; }
  }

  public static class List extends Obj {
    public final ArrayList<Obj> value = new ArrayList<Obj>();
    public List(Obj... args) {
      for (int i = 0; i < args.length; i += 2)
        value.add(args[i]);
    }
    public final Dict getMetaDict() { return LIST_META_DICT; }
  }

  public static class Dict extends Obj {
    public final HashMap<Obj, Obj> value = new HashMap<Obj, Obj>();
    public Dict(Obj... args) {
      if (args.length % 2 != 0)
        throw err("Dict.Dict args.length = " + Integer.toString(args.length));
      for (int i = 0; i < args.length; i += 2)
        value.put(args[i], args[i+1]);
    }
    public final Dict getMetaDict() { return DICT_META_DICT; }
  }

  public static abstract class Form extends Obj {
    public abstract Obj callForm(Context context, ArrayList<Ast> args);

    // Forms should not override 'call'.
    public final Obj call(Obj... args) { return super.call(args); }

    public final Dict getMetaDict() { return FORM_META_DICT; }
  }

  public static abstract class NamedForm extends Form {
    public final String name;
    public NamedForm(String n) { name = n; }
    public String toString() { return "<form " + name + ">"; }
  }

  public static abstract class Function extends Obj {
    // Functions should not override 'callForm'.
    public final Obj callForm(Context context, ArrayList<Ast> args) { return super.callForm(context, args); }

    public abstract Obj call(Obj... args);
  }

  public static abstract class NamedFunction extends Function {
    public final String name;
    public NamedFunction(String n) { name = n; }
    public String toString() { return "<function " + name + ">"; }
  }

  public static class Method extends Function {
    private final Obj owner, function;

    public Method(Obj o, Obj f) { owner = o; function = f; }

    public Obj call(Obj... args) {
      Obj[] newargs = new Obj[args.length+1];
      newargs[0] = owner;
      for (int i = 0; i < args.length; i++)
        newargs[i+1] = args[i];
      return function.call(newargs);
    }

    public Dict getMetaDict() { return METHOD_META_DICT; }

    public String toString() { return "<method " + function.toString() + ">"; }
  }

  public static abstract class Ast extends Obj {
    public final String source;
    public final int i;
    protected Ast(String s, int j) { source = s; i = j; }
    public abstract Obj eval(Context context);
    public Dict getMetaDict() { throw err("Ast.getMetaDict: TODO"); }
  }

  public static class NumAst extends Ast {
    public final Double value;
    public NumAst(String s, int j, Double v) { super(s, j); value = v; }
    public String toString() { return value.toString(); }
    public Obj eval(Context context) { return new Num(value); }
  }

  public static class StrAst extends Ast {
    public final String value;
    public StrAst(String s, int j, String v) { super(s, j); value = v; }
    public String toString() { return value.toString(); }
    public Obj eval(Context context) { return context.xlookup(this, value); }
  }

  public static class ListAst extends Ast {
    public final ArrayList<Ast> value = new ArrayList<Ast>();
    public ListAst(String s, int j) { super(s, j); }
    public void add(Ast item) { value.add(item); }
    public Ast pop() { return value.remove(value.size() - 1); }
    public int size() { return value.size(); }
    public String toString() { return value.toString(); }
    public Obj eval(Context context) {
      if (size() == 0)
        throw err("Tried to eval list of length zero. See line: " + Integer.toString(getLineCount(source, i)));

      Obj f = value.get(0).eval(context);

      if (f instanceof Form)
        return f.callForm(context, value);
      else {
        Obj[] args = new Obj[value.size() - 1];

        for (int i = 0; i < args.length; i++)
          args[i] = value.get(i+1).eval(context);

        return f.call(args);
      }
    }
  }

  public static final Context ROOT_CONTEXT = new RootContext();

  public static final Dict NUM_META_DICT = new Dict();
  public static final Dict STR_META_DICT = new Dict();
  public static final Dict LIST_META_DICT = new Dict();
  public static final Dict DICT_META_DICT = new Dict();
  public static final Dict METHOD_META_DICT = new Dict();
  public static final Dict FORM_META_DICT = new Dict();

  static {
    ROOT_CONTEXT.define(".", new NamedForm(".") {
      public Obj callForm(Context context, ArrayList<Ast> args) {
        Obj owner = args.get(1).eval(context);
        String attrname = args.get(2).toString();
        Obj attrfunction = owner.getMetaDict().value.get(attrname);
        if (attrfunction == null)
          throw err("Attribute '" + attrname + "' not found");
        return new Method(owner, attrfunction);
      }
    });
    ROOT_CONTEXT.define("begin", new NamedForm("begin") {
      public Obj callForm(Context context, ArrayList<Ast> args) {
        Obj last = new Num(0.0);
        for (int i = 1; i < args.size(); i++)
          last = args.get(i).eval(context);
        return last;
      }
    });
    ROOT_CONTEXT.define("quote", new NamedForm("quote") {
      public Obj callForm(Context context, ArrayList<Ast> args) { return args.get(1); }
    });
  }

  public static void main(String[] args) {
    System.out.println(parse(args[0]).eval(ROOT_CONTEXT));
  }
}
