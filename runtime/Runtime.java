package com.ccl;

import java.util.ArrayList;
import java.math.BigInteger;

// javac -d . *.java && java -cp . com.ccl.Runtime
public final class Runtime {

// Run all tests
public static void main(String[] args) {
  testAst();
}

/// Ast
public static void testAst() {
  // TODO: I really should do better testing here.
  parse("test.ccl");
  parse(
    "test.ccl " +
    "Expr Int 5");
}
public abstract static class Ast {}
public abstract static class Pattern extends Ast {}
public static final class List extends Pattern {
  public final ArrayList<Pattern> args;
  public final ArrayList<Pattern> optargs;
  public final String vararg; // nullable
  public List(
      ArrayList<Pattern> args,
      ArrayList<Pattern> optargs,
      String vararg) {
    this.args = args;
    this.optargs = optargs;
    this.vararg = vararg;
  }
}
public static final class Name extends Pattern {
  public final String name;
  public Name(String name) { this.name = name; }
}
public abstract static class Expression extends Ast {}
public static final class Int extends Expression {
  public final BigInteger value;
  public Int(BigInteger value) { this.value = value; }
}
public static final class Float extends Expression {
  public final Double value;
  public Float(Double value) { this.value = value; }
}
public static final class Str extends Expression {
  public final String value;
  public Str(String value) { this.value = value; }
}
public static final class Func extends Expression {
  public final boolean scope;
  public final Pattern args;
  public final Statement body;
  public Func(boolean scope, Pattern args, Statement body) {
    this.scope = scope;
    this.args = args;
    this.body = body;
  }
}
public static final class Find extends Expression {
  public final String name, message;
  public Find(String name, String message) {
    this.name = name;
    this.message = message;
  }
}
public static final class Callm extends Expression {
  public final String name;
  public final ArrayList<Expression> args;
  public final Expression vararg; // nullable
  public final String message;
  public Callm(
      String name, ArrayList<Expression> args, Expression vararg,
      String message) {
    this.name = name;
    this.args = args;
    this.vararg = vararg;
    this.message = message;
  }
}
public static final class Assign extends Expression {
  public final Pattern pattern;
  public final Expression value;
  public final String message;
  public Assign(Pattern pattern, Expression value, String message) {
    this.pattern = pattern;
    this.value =  value;
    this.message = message;
  }
}
public static final class Ife extends Expression {
  public final Expression cond, body, other;
  public Ife(Expression cond, Expression body, Expression other) {
    this.cond = cond;
    this.body = body;
    this.other = other;
  }
}
public static final class Or extends Expression {
  public final Expression left, right;
  public Or(Expression left, Expression right) {
    this.left = left;
    this.right = right;
  }
}
public static final class And extends Expression {
  public final Expression left, right;
  public And(Expression left, Expression right) {
    this.left = left;
    this.right = right;
  }
}
public static final class Import extends Expression {
  public final String name;
  public final String alias; // nullable
  public final String message;
  public Import(String name, String alias, String message) {
    this.name = name;
    this.alias = alias;
    this.message = message;
  }
}
public abstract static class Statement extends Ast {}
public static final class While extends Statement {
  public final Expression cond;
  public final Statement body;
  public While(Expression cond, Statement body) {
    this.cond = cond;
    this.body = body;
  }
}
public static final class Break extends Statement {}
public static final class Continue extends Statement {}
public static final class Ifs extends Statement {
  public final Expression cond;
  public final Statement body, other;
  public Ifs(Expression cond, Statement body, Statement other) {
    this.cond = cond;
    this.body = body;
    this.other = other;
  }
}
public static final class Block extends Statement {
  public final ArrayList<Statement> stmts;
  public Block(ArrayList<Statement> stmts) {
    this.stmts = stmts;
  }
}
public static final class Ret extends Statement {
  public final Expression expr;
  public Ret(Expression expr) { this.expr = expr; }
}
public static final class Expr extends Statement {
  public final Expression expr;
  public Expr(Expression expr) { this.expr = expr; }
}

public static final class Module extends Ast {
  public final String name;
  public final Block block;
  public Module(String name, Block block) {
    this.name = name;
    this.block = block;
  }
}

public static Module parse(String text) {
  return new Parser(text).parseModule();
}

// TODO: Erorr handling (e.g. early EOF)
private static final class Parser {
  private final String[] tokens;
  private int i = 0;
  public Parser(String text) { this.tokens = text.split("\\s+"); }
  public boolean hasNext() { return i < tokens.length; }
  private String next() { return tokens[i++]; }
  public Module parseModule() {
    String name = next();
    ArrayList<Statement> stmts = new ArrayList<Statement>();
    while (hasNext())
      stmts.add(parseStatement(next()));
    return new Module(name, new Block(stmts));
  }
  private String parseMessage() {
    String lookahead = next();
    StringBuilder sb = new StringBuilder();
    while (!lookahead.equals("End"))
      sb.append(lookahead + " ");
    return sb.toString().trim();
  }
  private Pattern parsePattern(String lookahead) {
    if (lookahead.equals("List")) {
      ArrayList<Pattern> args = new ArrayList<Pattern>();
      ArrayList<Pattern> optargs = new ArrayList<Pattern>();
      String vararg;
      while (!(lookahead = next()).equals("End"))
        args.add(parsePattern(lookahead));
      while (!(lookahead = next()).equals("End"))
        optargs.add(parsePattern(lookahead));
      vararg = next();
      if (vararg.equals("_"))
        vararg = null;
      return new List(args, optargs, vararg);
    }
    if (lookahead.equals("Name"))
      return new Name(next());
    throw new RuntimeException("Expected pattern: " + lookahead);
  }
  private Expression parseExpression(String lookahead) {
    if (lookahead.equals("Int"))
      return new Int(new BigInteger(next()));
    if (lookahead.equals("Float"))
      return new Float(Double.valueOf(next()));
    if (lookahead.equals("Str"))
      return new Str(next());
    if (lookahead.equals("Func")) {
      boolean scope = next().equals("true");
      Pattern args = parsePattern(next());
      Statement body = parseStatement(next());
      return new Func(scope, args, body);
    }
    if (lookahead.equals("Find")) {
      String name = next();
      String message = parseMessage();
      return new Find(name, message);
    }
    if (lookahead.equals("Callm")) {
      String name = next();
      ArrayList<Expression> args = new ArrayList<Expression>();
      while (!(lookahead = next()).equals("End"))
        args.add(parseExpression(lookahead));
      Expression vararg = parseExpression(next());
      if (vararg.equals("_"))
        vararg = null;
      String message = parseMessage();
      return new Callm(name, args, vararg, message);
    }
    if (lookahead.equals("Assign")) {
      Pattern pattern = parsePattern(next());
      Expression value = parseExpression(next());
      String message = parseMessage();
      return new Assign(pattern, value, message);
    }
    if (lookahead.equals("Ife")) {
      Expression cond = parseExpression(next());
      Expression body = parseExpression(next());
      Expression other = parseExpression(next());
      return new Ife(cond, body, other);
    }
    if (lookahead.equals("Or")) {
      Expression left = parseExpression(next());
      Expression right = parseExpression(next());
      return new Or(left, right);
    }
    if (lookahead.equals("And")) {
      Expression left = parseExpression(next());
      Expression right = parseExpression(next());
      return new And(left, right);
    }
    if (lookahead.equals("Import")) {
      String name = next();
      String alias = next();
      if (alias.equals("_"))
        alias = null;
      String message = parseMessage();
      return new Import(name, alias, message);
    }
    throw new RuntimeException("Expected expression: " + lookahead);
  }
  private Statement parseStatement(String lookahead) {
    if (lookahead.equals("While")) {
      Expression cond = parseExpression(next());
      Statement body = parseStatement(next());
      return new While(cond, body);
    }
    if (lookahead.equals("Break"))
      return new Break();
    if (lookahead.equals("Continue"))
      return new Continue();
    if (lookahead.equals("Ifs")) {
      Expression cond = parseExpression(next());
      Statement body = parseStatement(next());
      Statement other = parseStatement(next());
      return new Ifs(cond, body, other);
    }
    if (lookahead.equals("Block")) {
      ArrayList<Statement> stmts = new ArrayList<Statement>();
      while (!(lookahead = next()).equals("End"))
        stmts.add(parseStatement(lookahead));
      return new Block(stmts);
    }
    if (lookahead.equals("Ret"))
      return new Ret(parseExpression(next()));
    if (lookahead.equals("Expr"))
      return new Expr(parseExpression(next()));
    throw new RuntimeException("Expected statement: " + lookahead);
  }
}

}
