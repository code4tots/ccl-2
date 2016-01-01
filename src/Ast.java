import java.util.ArrayList;

public abstract class Ast {

  public final Token token;
  public Ast(Token token) { this.token = token; }
  public String getLocationString() {
    return "\nin " + token.getLocationString();
  }
  public abstract <T> T accept(AstVisitor<T> visitor);

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
    public final Ast cond;
    public final Ast body;
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
  public static final class Num extends Ast {
    public final Double val;
    public Num(Token token, Double val) {
      super(token);
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitNum(this);
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
  public static final class If extends Ast {
    public final Ast cond, body, other;
    public If(
        Token token, Ast cond, Ast body, Ast other) {
      super(token);
      this.cond = cond;
      this.body = body;
      this.other = other;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitIf(this);
    }
  }
  public static final class Assign extends Ast {
    public final String name;
    public final Ast val;
    public Assign(Token token, String name, Ast val) {
      super(token);
      this.name = name;
      this.val = val;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitAssign(this);
    }
  }
  public static final class Function extends Ast {
    public final ArrayList<String> args;
    public final String vararg;
    public final Ast body;
    public Function(
        Token token, ArrayList<String> args, String vararg, Ast body) {
      super(token);
      this.args = args;
      this.vararg = vararg;
      this.body = body;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitFunction(this);
    }
  }
  public static final class Call extends Ast {
    public final Ast f;
    public final ArrayList<Ast> args;
    public final Ast vararg;
    public Call(
        Token token, Ast f,
        ArrayList<Ast> args, Ast vararg) {
      super(token);
      this.f = f;
      this.args = args;
      this.vararg = vararg;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitCall(this);
    }
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
  public static final class SetItem extends Ast {
    public final Ast owner, index, value;
    public SetItem(
        Token token, Ast owner, Ast index, Ast value) {
      super(token);
      this.owner = owner;
      this.index = index;
      this.value = value;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitSetItem(this);
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
  public static final class Operation extends Ast {
    public final Ast owner;
    public final String name;
    public final ArrayList<Ast> args;
    public Operation(
        Token token, Ast owner, String name, Ast... args) {
      this(token, owner, name, toArrayList(args));
    }
    public Operation(
        Token token, Ast owner, String name, ArrayList<Ast> args) {
      super(token);
      this.owner = owner;
      this.name = name;
      this.args = args;
    }
    public <T> T accept(AstVisitor<T> visitor) {
      return visitor.visitOperation(this);
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
