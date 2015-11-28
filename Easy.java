import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

public abstract class Easy {

public final Scope ROOT_SCOPE = new Scope();

public abstract class Value {}

public final class Scope {
  public final Scope parent;
  public final Map<String, Value> table;
  public Scope() {
    this(null);
  }
  public Scope(Scope parent) {
    this.parent = parent;
    table = new HashMap<String, Value>();
  }
  public Value get(String name) {
    Value value = table.get(name);
    if (value == null) {
      if (parent == null)
        throw new RuntimeException(name);
      else
        return parent.get(name);
    }
    return value;
  }
  public Scope put(String name, Value value) {
    table.put(name, value);
    return this;
  }
}

public final class StackFrame {
  public final Bytecode[] bytecodes;
  public final Scope scope;
  public final Stack<Value> valueStack;
  public StackFrame(Bytecode[] bytecodes) {
    this(bytecodes, ROOT_SCOPE);
  }
  public StackFrame(Bytecode[] bytecodes, Scope parentScope) {
    this.bytecodes = bytecodes;
    this.scope = new Scope(parentScope);
    this.valueStack = new Stack<Value>();
  }
}

public final class Evaluator {
  public final Stack<StackFrame> stack;
  public int i;
  public Scope getScope() {
    return stack.peek().scope;
  }
  public Bytecode[] getBytecodes() {
    return stack.peek().bytecodes;
  }
  public Bytecode getBytecode() {
    return getBytecodes()[i];
  }
  public Evaluator(Bytecode[] bytecodes) {
    stack = new Stack<StackFrame>();
    stack.push(new StackFrame(bytecodes));
  }
  public Stack<Value> getValueStack() {
    return stack.peek().valueStack;
  }
  public void step() {
    getBytecode().step(this);
  }
  public boolean done() {
    return stack.size() == 1 && i >= getBytecodes().length;
  }
  public void run() {
    while (!done())
      step();
  }
}

/// As you can see, this class doesn't actually lex anything.
/// It is really just meant to mirror the Python Lexer class I wrote.
/// Its sole purpose here is for generating useful error messages.
public final class Lexer {
  public final String string;
  public final String filespec;
  public final Token[] tokens;
  public Lexer(String string, String filespec, int... ii) {
    this.string = string;
    this.filespec = filespec;
    tokens = new Token[ii.length];
    for (int i = 0; i < ii.length; i++)
      tokens[i] = new Token(this, ii[i]);
  }
}

public final class Token {
  public final Lexer lexer;
  public final int i;
  public Token(Lexer lexer, int i) {
    this.lexer = lexer;
    this.i = i;
  }
}

/// Bytecodes
public abstract class Bytecode {
  public final Token token;
  public Bytecode(Token token) {
    this.token = token;
  }
  public abstract void step(Evaluator ev);
}

public final class NameBytecode extends Bytecode {
  public final String name;
  public NameBytecode(Token token, String name) {
    super(token);
    this.name = name;
  }
  public void step(Evaluator ev) {
    ev.getValueStack().push(ev.getScope().get(name));
  }
}

public final class NumberBytecode extends Bytecode {
  public final double value;
  public NumberBytecode(Token token, double value) {
    super(token);
    this.value = value;
  }
  public void step(Evaluator ev) {
    // TODO
  }
}

public final class CallBytecode extends Bytecode {
  public final int argumentLength;
  public final boolean useVararg;
  public CallBytecode(Token token, int argumentLength, boolean useVararg) {
    super(token);
    this.argumentLength = argumentLength;
    this.useVararg = useVararg;
  }
  public void step(Evaluator ev) {
    // TODO
  }
}

}
