import java.util.ArrayList;
import java.math.BigInteger;

public abstract class Ast implements Traceable {

  public final Token token;
  public Ast(Token token) { this.token = token; }
  public String getTraceMessage() {
    return "\nin expression in " + token.getLocationString();
  }
  public abstract <T> T accept(AstVisitor<T> visitor);

  // Statement only
  public static final class Return extends Ast {
    public final Ast val;
    public Return(Token token, Ast val) {
      super(token);
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitReturn(this);
    }
  }
  public static final class While extends Ast {
    public final Ast cond, body;
    public While(Token token, Ast cond, Ast body) {
      super(token);
      this.cond = cond;
      this.body = body;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitWhile(this);
    }
  }
  public static final class Block extends Ast {
    public final ArrayList<Ast> body;
    public Block(Token token, ArrayList<Ast> body) {
      super(token);
      this.body = body;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitBlock(this);
    }
  }
  public static final class Break extends Ast {
    public Break(Token token) { super(token); }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitBreak(this);
    }
  }
  public static final class Continue extends Ast {
    public Continue(Token token) { super(token); }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitContinue(this);
    }
  }
  public static final class ExpressionStatement extends Ast {
    public final Ast expr;
    public ExpressionStatement(Token token, Ast expr) {
      super(token);
      this.expr = expr;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitExpressionStatement(this);
    }
  }

  // Statement or Expression
  public static final class If extends Ast {
    public final Ast cond, body, other;
    public final boolean expr;
    public If(
        Token token, Ast cond, Ast body, Ast other,
        boolean expr) {
      super(token);
      this.cond = cond;
      this.body = body;
      this.other = other;
      this.expr = expr;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitIf(this);
    }
  }

  // Expression only
  public static final class Int extends Ast {
    public final BigInteger val;
    public Int(Token token, BigInteger val) {
      super(token);
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitInt(this);
    }
  }
  public static final class Flt extends Ast {
    public final Double val;
    public Flt(Token token, Double val) {
      super(token);
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitFlt(this);
    }
  }
  public static final class Str extends Ast {
    public final String val;
    public Str(Token token, String val) {
      super(token);
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitStr(this);
    }
  }
  public static final class Name extends Ast {
    public final String name;
    public Name(Token token, String name) {
      super(token);
      this.name = name;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitName(this);
    }
  }
  public static final class Assign extends Ast {
    public final Pattern pattern;
    public final Ast val;
    public Assign(Token token, Pattern pattern, Ast val) {
      super(token);
      this.pattern = pattern;
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitAssign(this);
    }
  }
  public static final class Function extends Ast {
    public final ListPattern args;
    public final Ast body;
    public final boolean newScope;
    public Function(
        Token token, ListPattern args, Ast body, boolean newScope) {
      super(token);
      this.args = args;
      this.body = body;
      this.newScope = newScope;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitFunction(this);
    }
  }
  public abstract static class Pattern {
    public abstract <T> void accept(PatternVisitor<T> visitor, T t);
  }
  public static final class ListPattern extends Pattern {
    public final ArrayList<Pattern> args;
    public final ArrayList<Pattern> optargs;
    public final String vararg;
    public ListPattern(
        ArrayList<Pattern> args, ArrayList<Pattern> optargs, String vararg) {
      this.args = args;
      this.optargs = optargs;
      this.vararg = vararg;
    }
    public <T> void accept(PatternVisitor<T> visitor, T t) {
      visitor.visitListPattern(this, t);
    }
  }
  public static final class NamePattern extends Pattern {
    public final String name;
    public NamePattern(String name) { this.name = name; }
    public <T> void accept(PatternVisitor<T> visitor, T t) {
      visitor.visitNamePattern(this, t);
    }
  }
  public abstract static class PatternVisitor<T> {
    public final void visit(Pattern pattern, T t) { pattern.accept(this, t); }
    public abstract void visitListPattern(ListPattern pattern, T t);
    public abstract void visitNamePattern(NamePattern pattern, T t);
  }
  public static final class GetMethod extends Ast {
    public final Ast owner;
    public final String name;
    public GetMethod(Token token, Ast owner, String name) {
      super(token);
      this.owner = owner;
      this.name = name;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitGetMethod(this);
    }
  }
  public static final class GetAttribute extends Ast {
    public final Ast owner;
    public final String name;
    public GetAttribute(Token token, Ast owner, String name) {
      super(token);
      this.owner = owner;
      this.name = name;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitGetAttribute(this);
    }
  }
  public static final class SetAttribute extends Ast {
    public final Ast owner;
    public final String name;
    public final Ast val;
    public SetAttribute(
        Token token, Ast owner, String name, Ast val) {
      super(token);
      this.owner = owner;
      this.name = name;
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitSetAttribute(this);
    }
  }
  public static final class Is extends Ast {
    public final Ast left, right;
    public Is(Token token, Ast left, Ast right) {
      super(token);
      this.left = left;
      this.right = right;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitIs(this);
    }
  }
  public static final class IsNot extends Ast {
    public final Ast left, right;
    public IsNot(Token token, Ast left, Ast right) {
      super(token);
      this.left = left;
      this.right = right;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitIsNot(this);
    }
  }
  public static final class Call extends Ast {
    public final Ast owner;
    public final String name; // method name
    public final ArrayList<Ast> args;
    public final Ast vararg;
    public Call(
        Token token, Ast owner, String name, Ast... args) {
      this(token, owner, name, toArrayList(args));
    }
    public Call(
        Token token, Ast owner, String name, ArrayList<Ast> args) {
      this(token, owner, name, args, null);
    }
    public Call(
        Token token, Ast owner, String name,
        ArrayList<Ast> args, Ast vararg) {
      super(token);
      this.owner = owner;
      this.name = name;
      this.args = args;
      this.vararg = vararg;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitCall(this);
    }
  }
  public static final class Not extends Ast {
    public final Ast target;
    public Not(Token token, Ast target) {
      super(token);
      this.target = target;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitNot(this);
    }
  }
  public static final class And extends Ast {
    public final Ast left, right;
    public And(Token token, Ast left, Ast right) {
      super(token);
      this.left = left;
      this.right = right;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitAnd(this);
    }
  }
  public static final class Or extends Ast {
    public final Ast left, right;
    public Or(Token token, Ast left, Ast right) {
      super(token);
      this.left = left;
      this.right = right;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitOr(this);
    }
  }

  // Module
  public static final class Module extends Ast {
    public final String name;
    public final Ast body;
    public Module(Token token, String name, Ast body) {
      super(token);
      this.name = name;
      this.body = body;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitModule(this);
    }
  }

  public static ArrayList<Ast> toArrayList(Ast... args) {
    ArrayList<Ast> al = new ArrayList<Ast>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }

}
