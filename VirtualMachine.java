import java.util.Stack;
import java.util.HashMap;

public class VirtualMachine {

  private final Opcode[] opcodes;
  private int programCounter;
  private Scope scope;
  private Stack<Value> valueStack;
  private Stack<Scope> scopeStack;
  private Stack<Integer> returnProgramCounterStack;
  private int[] jumpTable;

  public VirtualMachine(Opcode[] opcodes) {
    this.opcodes = opcodes;
    programCounter = 0;
    scope = new Scope(null);
    valueStack = new Stack<Value>();
    scopeStack = new Stack<Scope>();
    returnProgramCounterStack = new Stack<Integer>();
    jumpTable = new int[opcodes.length];

    // Fill jumpTable
    Stack<Integer> indexStack = new Stack<Integer>();
    Stack<Opcode> opcodeStack = new Stack<Opcode>();
    for (int i = 0; i < opcodes.length; i++) {
      if (opcodes[i] instanceof StartFunction) {
        indexStack.push(i);
        opcodeStack.push(opcodes[i]);
      }

      if (opcodes[i] instanceof StartBlock) {
        indexStack.push(i);
        opcodeStack.push(opcodes[i]);
      }

      if (opcodes[i] instanceof EndFunction) {
        jumpTable[indexStack.pop()] = i;
        if (!(opcodeStack.pop() instanceof StartFunction))
          throw new RuntimeException();
      }

      if (opcodes[i] instanceof EndBlock) {
        jumpTable[indexStack.pop()] = i;
        if (!(opcodeStack.pop() instanceof StartBlock))
          throw new RuntimeException();
      }
    }
  }

  public Opcode nextOpcode() {
    return opcodes[programCounter++];
  }

  public void returnFromFunction() {
    programCounter = returnProgramCounterStack.pop();
    scopeStack.pop();
  }

  public void callFunction() {

    Value callable = valueStack.pop();

    if (callable instanceof Lambda) {
      Lambda lambda = (Lambda) callable;
      returnProgramCounterStack.push(programCounter+1);
      programCounter = lambda.programCounter;
      scopeStack.push(new Scope(lambda.parentScope));
      return;
    }

    // TODO: Builtin functions.

    throw new RuntimeException(callable.getClass().getName());
  }

  public void step() {
    Opcode opcode = nextOpcode();

    // Create a new function.
    if (opcode instanceof StartFunction) {
      valueStack.push(new Lambda(scope, programCounter));
      programCounter = jumpTable[programCounter-1];
      return;
    }

    // Return from a function.
    if (opcode instanceof EndFunction || opcode instanceof Return) {
      returnFromFunction();
      return;
    }

    // Invoke a function.
    if (opcode instanceof CallFunction) {
      callFunction();
    }

    throw new RuntimeException(opcode.getClass().getName());
  }

  static private Opcode[] opcodeStackToOpcodeArray(Stack<Opcode> stack) {
    Opcode[] array = new Opcode[stack.size()];
    for (int i = 0; i < array.length; i++)
      array[i] = stack.get(i);
    return array;
  }

  public static void main(String[] args) {
  }

// Maybe should be in separate files.

static public abstract class Opcode {}
static public final class Return extends Opcode {}
static public final class CallFunction extends Opcode {}
static public final class StartFunction extends Opcode {}
static public final class EndFunction extends Opcode {}
static public final class StartBlock extends Opcode {}
static public final class EndBlock extends Opcode {}

// debugging opcodes.

static public final class Scope {
  private final Scope parent;
  private final HashMap<String, Value> table;
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
  public void put(String name, Value value) {
    table.put(name, value);
  }
}

static public abstract class Value {}
static public final class Nil extends Value {}
static public final Nil nil = new Nil();
static public final class Lambda extends Value {

  // The lexcial scope surrounding this function.
  public final Scope parentScope;

  // Where the function starts in the opcode array.
  // Note that this is the opcode one past the 'StartFunction' opcode.
  public final int programCounter;

  public Lambda(Scope parentScope, int programCounter) {
    this.parentScope = parentScope;
    this.programCounter = programCounter;
  }
}

}
