import java.util.Stack; // Ugh. There's no ArrayList equivalent stack-like subclass.
import java.util.ArrayList;
import java.util.HashMap;

public class Blarg {

// ---

static public abstract class AstEvaluator {
  public abstract void step();
  public abstract boolean done();
  public void run() {
    while (!done())
      step();
  }
}

static public abstract class Ast {
  public abstract AstEvaluator getBoundEvaluator();
}

static public final class If extends Ast {
  public final Ast condition, body, otherwise;
  public If(Ast condition, Ast body, Ast otherwise) {
    this.condition = condition;
    this.body = body;
    this.otherwise = otherwise;
  }

  static public final class IfEvaluator {
    public final If ast;
    public IfEvaluator(If ast) {
      this.ast = ast;
    }
  }

  public AstEvaluator getBoundEvaluator() {
    return new IfEvaluator(this);
  }
}

static public final class IntegerAst extends Ast {
  public final Integer value;
  public IntegerAst(Integer value) {
    this.value = value;
  }

  public AstEvaluator getBoundEvaluator() {
    return new IntegerAstEvaluator(this);
  }

  static public final class IntegerAstEvaluator extends AstEvaluator {
    public final 
  }
}

static public final class DoubleAst extends Ast {
  public final Double value;
  public DoubleAst(Dobule value) {
    this.value = value;
  }
}

}
