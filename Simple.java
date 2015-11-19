import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Simple {

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

  // object system
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

  // reader/parser

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

    public Token makeSymbolToken() {
      Token token = new Symbol(sliceCurrentText());
      start = end;
      return token;
    }

    public Token makeNumberToken(Double value) {
      Token token = new Number(value);
      start = end;
      return token;
    }

    public Token makeStringToken(String value) {
      Token token = new Text(value);
      start = end;
      return token;
    }

    public static boolean isSymbolCharacter(char c) {
      return Character.isAlphabetic(c) || Character.isDigit(c) ||
          c == '%' || c == '*' || c == '+' || c == '-' ||
          c == '<' || c == '=' || c == '>' || c == '&';
    }

    public Token extractNext() {
      if (finished)
        throw new RuntimeException("No more tokens");

      skipSpacesAndComments();

      if (end >= string.length()) {
        finished = true;
        return null;
      }

      char c = string.charAt(end);

      if (c == '.' || c == '(' || c == ')' || c == '{' || c == '}' || c == '/') {
        end++;
        return makeSymbolToken();
      }

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

      if (c == '"' || c == '\'') {
        char q = c;
        end++;
        StringBuilder sb = new StringBuilder();
        while (end < string.length() && string.charAt(end) != q) {
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
            case '"':
              sb.append('"');
              break;
            case '\'':
              sb.append('\'');
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
        return makeSymbolToken();

      while (end < string.length() && !Character.isWhitespace(string.charAt(end)))
        end++;

      throw new RuntimeException("Unrecognized token '" + string.substring(start, end) + "'");
    }
  }

  public static ArrayList<Token> parse(String string) {
    TokenStream tokenStream = new TokenStream(string);
    ArrayList<Token> tokens = new ArrayList<Token>();
    while (!tokenStream.finished)
      tokens.add(tokenStream.next());
    return tokens;
  }

  // runtime

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

 public static class Evaluator {
    private Stack<StackFrame> callStack;
    private Stack<Stack<Value>> evalStackStack;
    private ArrayList<Token> tokens;
    private int programCounter;

    public Evaluator(String program) {
      this(parse(program));
    }

    public Evaluator(ArrayList<Token> tokens) {
      callStack = new Stack<StackFrame>();
      callStack.push(new StackFrame("<global>"));
      evalStackStack = new Stack<Stack<Value>>();
      evalStackStack.push(new Stack<Value>());
      this.tokens = tokens;
      programCounter = 0;
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
      } else if (token instanceof Symbol) {
        String name = token.getString();
        if (name.equals("{")) {
          // lambda
          int depth = 1, pc = programCounter;
          while (depth > 0) {
            String value = nextToken().getString();
            if (value.equals("{"))
              depth++;
            else if (value.equals("}"))
              depth--;
          }
          pushValue(new Lambda(pc, callStack.peek()));
        } else if (name.equals("}")) {
          // return from function call
          programCounter = callStack.pop().returnProgramCounter;
        } else if (name.equals("=")) {
          // assignment
          name = nextToken().getString();
          assignVariable(name, popValue());
        } else if (name.equals(".")) {
          // method call
          name = nextToken().getString();
          if (name.equals("print"))
            System.out.println(popValue());
          else // TODO
            throw new RuntimeException("Cannot call method " + name);
        } else if (name.equals("/")) {
          // function call
          Lambda lambda = (Lambda) popValue();
          callStack.push(new StackFrame("<lambda>", programCounter+1, lambda.parentStackFrame));
          programCounter = lambda.programCounter;
        } else {
          // variable lookup
          System.out.println("Pushing value of " + name);
          pushValue(lookupVariable(name));
        }
      } else {
        throw new RuntimeException("Cannot evaluate token of type " + token.getClass().getName());
      }
    }

    public void run() {
      while (!done())
        step();
    }
  }

  public static void main(String[] args) {
    new Evaluator(
      "5 = x x.print " +
      "{ 'hi'.print } = say-hi " +
      "say-hi/ say-hi/"
    ).run();
  }
}
