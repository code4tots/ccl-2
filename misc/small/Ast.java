package com.ccl;

import java.util.ArrayList;
import java.math.BigInteger;

public abstract class Ast implements Traceable {

  public final Token token;
  public Ast(Token token) { this.token = token; }
  public String getTraceMessage() {
    return "\nin expression in " + token.getLocationString();
  }
  public <T> T as(Class<T> cls) { return cls.cast(this); }

  // Statement
  public abstract static class Statement extends Ast {
    public Statement(Token token) { super(token); }
    public void run(Context ctx) { throw new Err("TODO"); }
  }
  public static final class Return extends Statement {
    public final Expression val;
    public Return(Token token, Expression val) {
      super(token);
      this.val = val;
    }
  }
  public static final class While extends Statement {
    public final Expression cond;
    public final Statement body;
    public While(Token token, Expression cond, Statement body) {
      super(token);
      this.cond = cond;
      this.body = body;
    }
  }
  public static final class Block extends Statement {
    public final ArrayList<Statement> body;
    public Block(Token token, ArrayList<Statement> body) {
      super(token);
      this.body = body;
    }
  }
  public static final class Break extends Statement {
    public Break(Token token) { super(token); }
  }
  public static final class Continue extends Statement {
    public Continue(Token token) { super(token); }
  }
  public static final class ExpressionStatement extends Statement {
    public final Expression expr;
    public ExpressionStatement(Token token, Expression expr) {
      super(token);
      this.expr = expr;
    }
  }
  public static final class If extends Statement {
    public final Expression cond;
    public final Statement body;
    public final Statement other; // nullable
    public If(Token token, Expression cond, Statement body, Statement other) {
      super(token);
      this.cond = cond;
      this.body = body;
      this.other = other;
    }
  }

  // Expression only
  public abstract static class Expression extends Ast {
    public Expression(Token token) { super(token); }
    public Value eval(Scope scope) { throw new Err("TODO"); }
  }
  public static final class Ternary extends Expression {
    public final Expression cond, body, other;
    public Ternary(
        Token token, Expression cond, Expression body, Expression other) {
      super(token);
      this.cond = cond;
      this.body = body;
      this.other = other;
    }
  }
  public static final class Int extends Expression {
    public final BigInteger val;
    public Int(Token token, BigInteger val) {
      super(token);
      this.val = val;
    }
  }
  public static final class Flt extends Expression {
    public final Double val;
    public Flt(Token token, Double val) {
      super(token);
      this.val = val;
    }
  }
  public static final class Str extends Expression {
    public final String val;
    public Str(Token token, String val) {
      super(token);
      this.val = val;
    }
  }
  public static final class Name extends Expression {
    public final String name;
    public Name(Token token, String name) {
      super(token);
      this.name = name;
    }
  }
  public static final class Assign extends Expression {
    public final Pattern pattern;
    public final Expression val;
    public Assign(Token token, Pattern pattern, Expression val) {
      super(token);
      this.pattern = pattern;
      this.val = val;
    }
  }
  public static final class Function extends Expression {
    public final ListPattern args;
    public final Statement body;
    public final boolean newScope;
    public Function(
        Token token, ListPattern args, Statement body, boolean newScope) {
      super(token);
      this.args = args;
      this.body = body;
      this.newScope = newScope;
    }
  }
  public abstract static class Pattern {
    public abstract void assign(Scope scope, Value value);
  }
  public static final class ListPattern extends Pattern {
    public final ArrayList<Pattern> args;
    public final ArrayList<Pattern> optargs;
    public final String vararg; // nullable
    public ListPattern(
        ArrayList<Pattern> args, ArrayList<Pattern> optargs, String vararg) {
      this.args = args;
      this.optargs = optargs;
      this.vararg = vararg;
    }
    public void assign(Scope scope, Value value) {
      throw new Err("TODO");
    }
  }
  public static final class NamePattern extends Pattern {
    public final String name;
    public NamePattern(String name) { this.name = name; }
    public void assign(Scope scope, Value value) { scope.put(name, value); }
  }
  public static final class GetMethod extends Expression {
    public final Expression owner;
    public final String name;
    public GetMethod(Token token, Expression owner, String name) {
      super(token);
      this.owner = owner;
      this.name = name;
    }
  }
  public static final class GetAttribute extends Expression {
    public final Expression owner;
    public final String name;
    public GetAttribute(Token token, Expression owner, String name) {
      super(token);
      this.owner = owner;
      this.name = name;
    }
  }
  public static final class SetAttribute extends Expression {
    public final Expression owner;
    public final String name;
    public final Expression val;
    public SetAttribute(
        Token token, Expression owner, String name, Expression val) {
      super(token);
      this.owner = owner;
      this.name = name;
      this.val = val;
    }
  }
  public static final class Is extends Expression {
    public final Expression left, right;
    public Is(Token token, Expression left, Expression right) {
      super(token);
      this.left = left;
      this.right = right;
    }
  }
  public static final class IsNot extends Expression {
    public final Expression left, right;
    public IsNot(Token token, Expression left, Expression right) {
      super(token);
      this.left = left;
      this.right = right;
    }
  }
  public static final class Call extends Expression {
    public final Expression owner;
    public final String name; // method name
    public final ArrayList<Expression> args;
    public final Expression vararg; // nullable
    public Call(
        Token token, Expression owner, String name, Expression... args) {
      this(token, owner, name, toArrayList(args));
    }
    public Call(
        Token token, Expression owner, String name,
        ArrayList<Expression> args) {
      this(token, owner, name, args, null);
    }
    public Call(
        Token token, Expression owner, String name,
        ArrayList<Expression> args, Expression vararg) {
      super(token);
      this.owner = owner;
      this.name = name;
      this.args = args;
      this.vararg = vararg;
    }
  }
  public static final class Not extends Expression {
    public final Ast target;
    public Not(Token token, Ast target) {
      super(token);
      this.target = target;
    }
  }
  public static final class And extends Expression {
    public final Ast left, right;
    public And(Token token, Ast left, Ast right) {
      super(token);
      this.left = left;
      this.right = right;
    }
  }
  public static final class Or extends Expression {
    public final Ast left, right;
    public Or(Token token, Ast left, Ast right) {
      super(token);
      this.left = left;
      this.right = right;
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
  }

  public static ArrayList<Expression> toArrayList(Expression... args) {
    ArrayList<Expression> al = new ArrayList<Expression>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }

}
