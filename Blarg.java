import java.util.HashMap;
import java.util.Stack;

public class Blarg {

  public static class Evaluator {
    private Stack<StackFrame> callStack = new Stack<StackFrame>();
    private Stack<Stack<Value>> evalStackStack = new Stack<Stack<Value>>();
    private Stack<Token> tokens;
    private int programCounter = 0;

    public Evaluator(String programSource) {
      this(parse(programSource));
    }

    public Evaluator(Stack<Token> tokens) {
      this.tokens = tokens;
      evalStackStack.push(new Stack<Value>());
    }

    public void pushValue(Value value) {
      evalStackStack.peek().push(value);
    }

    public Value popValue() {
      return evalStackStack.peek().pop();
    }

    public void pushStack() {
      evalStackStack.push(new Stack<Value>());
    }

    public Stack<Value> popStack() {
      return evalStackStack.pop();
    }

    public Value lookupVariable(String name) {
      return callStack.peek().lookupVariable(name);
    }

    public void assignVariable(String name, Value value) {
      callStack.peek().assignVariable(name, value);
    }

    public boolean done() {
      return programCounter >= tokens.size();
    }

    public Token nextToken() {
      return tokens.get(programCounter++);
    }

    public void step() {
      if (done())
        return;

      Token token = nextToken();

      if (token instanceof Text || token instanceof Number) {
        pushValue(token);
        return;
      }

      if (token instanceof Symbol) {

        String name = token.getString();

        // variable assignment
        if (name.equals("=")) {
          name = nextToken().getString();
          assignVariable(name, popValue());
          return;
        }

        // lambda literal
        if (name.equals("{")) {
          int depth = 1, pc = programCounter;
          while (depth > 0) {
            String str = nextToken().getString();
            if (str.equals("{"))
              depth++;
            else if (str.equals("}"))
              depth--;
          }
          pushValue(new Lambda(pc, callStack.peek()));
          return;
        }

        // return from a function call
        if (name.equals("}")) {
          programCounter = callStack.pop().returnProgramCounter;
          return;
        }

        // assignment
        if (name.equals("=")) {
          name = nextToken().getString();
          assignVariable(name, popValue());
          return;
        }

        // method call
        if (name.equals(".")) {
          name = nextToken().getString();
          Value owner = popValue();
          // TODO
          if (name.equals("print")) {
            System.out.println(owner);
            return;
          }
          throw new RuntimeException("Cannot call method " + name);
        }

        // function call
        if (name.equals("/")) {
          Lambda lambda = (Lambda) popValue();
          callStack.push(new StackFrame("<lamda>", programCounter+1, lambda.parentStackFrame));
          programCounter = lambda.programCounter;
          return;
        }

        // If it's not a special symbol, do a variable lookup
        pushValue(lookupVariable(name));
        return;
      }

      throw new RuntimeException("Cannot evaluate token of type " + token.getClass().getName());
    }

    public void run() {
      while (!done())
        step();
    }
  }

  public static class StackFrame {
    public final String name; // for debugging purposes
    public final int returnProgramCounter;
    public final StackFrame parent;
    private final HashMap<String, Value> scope;

    public StackFrame(String name) {
      this(name, 0, null);
    }

    public StackFrame(String name, int returnProgramCounter, StackFrame parent) {
      this.name = name;
      this.returnProgramCounter = returnProgramCounter;
      this.parent = parent;
      scope = new HashMap<String, Value>();
    }

    public Value lookupVariable(String name) {
      Value value = scope.get(name);

      if (value == null)
        if (parent == null)
          throw new VariableNotFoundException(name);
        else
          value = parent.lookupVariable(name);

      return value;
    }

    public void assignVariable(String name, Value value) {
      // welcome to the Python school of assigning values to variables.
      scope.put(name, value);
    }
  }
  public static abstract class Value {
    public String getString() {
      throw new NotImplementedException(getClass().getName(), "getString");
    }
    public Double getNumber() {
      throw new NotImplementedException(getClass().getName(), "getNumber");
    }
  }

  public static abstract class Token extends Value {}

  public static final class Symbol extends Token {
    private final String string;
    public Symbol(String string) {
      this.string = string;
    }
    public String getString() {
      return string;
    }
    public String toString() {
      return string;
    }
  }

  public static final class Number extends Token {
    private final Double number;
    public Number(Double number) {
      this.number = number;
    }
    public Double getNumber() {
      return number;
    }
    public String toString() {
      return number.toString();
    }
  }

  public static final class Text extends Token {
    private final String string;
    public Text(String string) {
      this.string = string;
    }
    public String getString() {
      return string;
    }
    public String toString() {
      return string;
    }
  }

  public static final class Lambda extends Value {
    public final int programCounter;
    public final StackFrame parentStackFrame;
    public Lambda(int programCounter, StackFrame parentStackFrame) {
      this.programCounter = programCounter;
      this.parentStackFrame = parentStackFrame;
    }
  }

  // exceptions
  public static class VariableNotFoundException extends RuntimeException {
    public VariableNotFoundException(String name) {
      super(name);
    }
  }

  public static class NotImplementedException extends RuntimeException {
    public NotImplementedException(String className, String methodName) {
      super(className + "." + methodName);
    }
  }

  public static Stack<Token> parse(String s) {
    Stack<Token> tokens = new Stack<Token>();
    int i = 0;

    while (true) {
      while (i < s.length() && Character.isWhitespace(s.charAt(i)))
        i++;

      if (i >= s.length())
        break;

      if (s.startsWith("r'", i) || s.startsWith("r\"", i) || s.startsWith("'", i) || s.startsWith("\"", i)) {
        boolean raw = false;
        if (s.charAt(i) == 'r') {
          raw = true;
          i++;
        }
        int j = s.startsWith("'''", i) || s.startsWith("\"\"\"", i) ? i + 3 : i + 1;
        String quote = s.substring(i, j);
        i = j;
        StringBuilder sb = new StringBuilder();
        while (!s.startsWith(quote, i)) {
          if (i >= s.length())
            throw new RuntimeException("");
          char c = s.charAt(i++);
          if (c == '\\') {
            c = s.charAt(i++);
            switch(c) {
            case 'n': sb.append('\n');
            case 't': sb.append('\t');
            case '\\': sb.append('\\');
            case '"': sb.append('"');
            case '\'': sb.append('\'');
            default: throw new RuntimeException(Character.toString(c));
            }
          } else sb.append(c);
        }
        i += quote.length();
        tokens.push(new Text(sb.toString()));
        continue;
      }

      int j = i;
      char c = s.charAt(i);

      if (c == '+' || c == '-' || Character.isDigit(c) || c == '.') {
        if (c == '+' || c == '-')
          i++;

        boolean digitSeen = false;

        while (i < s.length() && Character.isDigit(s.charAt(i))) {
          digitSeen = true;
          i++;
        }

        if (s.charAt(i) == '.')
          i++;

        while (i < s.length() && Character.isDigit(s.charAt(i))) {
          digitSeen = true;
          i++;
        }

        if (digitSeen) {
          tokens.push(new Number(Double.valueOf(s.substring(j, i))));
          continue;
        }
        else
          i = j;
      }

      while (isSymbolCharacter(s.charAt(i)))
        i++;

      if (j != i) {
        tokens.push(new Symbol(s.substring(j, i)));
        continue;
      }


      while (i < s.length() && !Character.isWhitespace(s.charAt(i)))
        i++;

      throw new RuntimeException("Unrecognzed token " + s.substring(j, i));
    }

    return tokens;
  }
      
  public static boolean isSymbolCharacter(char c) {
    return Character.isAlphabetic(c) || Character.isDigit(c) ||
        c == '%' || c == '*' || c == '+' || c == '-' ||
        c == '<' || c == '=' || c == '>' || c == '&';
  }

  public static void main(String[] args) {}
}
