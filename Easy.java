public abstract class Easy {

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
}

public final class NameBytecode extends Bytecode {
  public final String name;
  public NameBytecode(Token token, String name) {
    super(token);
    this.name = name;
  }
}

public final class NumberBytecode extends Bytecode {
  public final double value;
  public NumberBytecode(Token token, double value) {
    super(token);
    this.value = value;
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
}

}
