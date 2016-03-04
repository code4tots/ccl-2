package com.ccl.core;

import java.util.ArrayList;

public class Evaluator extends AstVisitor<Value> {

  private final Runtime runtime;
  private final Scope scope;
  private boolean breakFlag = false, continueFlag = false, returnFlag = false;

  public Evaluator(Runtime runtime, Scope scope) {
    this.runtime = runtime;
    this.scope = scope;
  }

  private boolean anyFlag() {
    return breakFlag || continueFlag || returnFlag;
  }

  // Statement only
  public Value visitReturn(Ast.Return node) {
    Value value = visit(node.val);
    returnFlag = true;
    return value;
  }

  public Value visitIf(Ast.If node) {
    return visit(visit(node.cond).truthy() ? node.body : node.other);
  }

  public Value visitWhile(Ast.While node) {
    while (visit(node.cond).truthy()) {
      Value value = visit(node.body);
      if (breakFlag) {
        breakFlag = false;
        break;
      }
      if (continueFlag) {
        continueFlag = false;
        continue;
      }
      if (returnFlag) {
        return value;
      }
    }
    return null;
  }

  public Value visitBlock(Ast.Block node) {
    ArrayList<Ast> body = node.body;
    int size = body.size();
    for (int i = 0; i < size; i++) {
      Value value = visit(body.get(i));
      if (anyFlag()) {
        return value;
      }
    }
    return null;
  }

  public Value visitBreak(Ast.Break node) {
    breakFlag = true;
    return null;
  }

  public Value visitContinue(Ast.Continue node) {
    continueFlag = true;
    return null;
  }

  public Value visitExpressionStatement(
      Ast.ExpressionStatement node) {
    return visit(node.expr);
  }

  // Expression only
  public Value visitInt(Ast.Int node) {
    return Number.from(node.val.doubleValue());
  }

  public Value visitFlt(Ast.Flt node) {
    return Number.from(node.val);
  }

  public Value visitStr(Ast.Str node) {
    return Text.from(node.val);
  }

  public Value visitName(Ast.Name node) {
    try {
      return scope.get(node.name);
    } catch (final Err e) {
      e.add(node);
      throw e;
    }
  }

  public Value visitAssign(Ast.Assign node) {
    Value value = visit(node.val);
    new Assigner(scope).visit(node.pattern, value);
    return value;
  }

  public Value visitFunction(Ast.Function node) {
    return new UserFunction(node, scope, runtime);
  }

  public Value visitCall(Ast.Call node) {
    Value owner = visit(node.owner);
    ArrayList<Value> arglist = new ArrayList<Value>();
    for (int i = 0; i < node.args.size(); i++) {
      arglist.add(visit(node.args.get(i)));
    }
    if (node.vararg != null) {
      arglist.addAll(
          visit(node.vararg)
              .as(List.class)
              .getValue());
    }
    List args = List.from(arglist);
    try {
      if (node.name == null) {
        if (owner instanceof Function) {
          return ((Function) owner).call(owner, args);
        } else {
          return owner.call("__call__", args);
        }
      } else {
        return owner.call(node.name, List.from(arglist));
      }
    } catch (final Err e) {
      e.add(node);
      throw e;
    // } catch (final Throwable e) {
    //   throw new Err(e, node);
    }
  }

  public Value visitGetMethod(Ast.GetMethod node) {
    throw new Err("'GetMethod' not implemented");
  }

  public Value visitGetAttribute(Ast.GetAttribute node) {
    return visit(node.owner).as(Blob.class).getattr(node.name);
  }

  public Value visitSetAttribute(Ast.SetAttribute node) {
    Value value = visit(node.val);
    visit(node.owner).as(Blob.class).setattr(node.name, value);
    return value;
  }

  public Value visitIs(Ast.Is node) {
    return visit(node.left) == visit(node.right) ? Bool.yes : Bool.no;
  }

  public Value visitIsNot(Ast.IsNot node) {
    return visit(node.left) == visit(node.right) ? Bool.yes : Bool.no;
  }

  public Value visitNot(Ast.Not node) {
    return visit(node.target).truthy() ? Bool.no : Bool.yes;
  }

  public Value visitAnd(Ast.And node) {
    Value value = visit(node.left);
    if (!value.truthy()) {
      return value;
    }
    return visit(node.right);
  }

  public Value visitOr(Ast.Or node) {
    Value value = visit(node.left);
    if (value.truthy()) {
      return value;
    }
    return visit(node.right);
  }

  public Value visitTernary(Ast.Ternary node) {
    return visit(visit(node.cond).truthy() ? node.body : node.other);
  }

  // Module
  public Value visitModule(Ast.Module node) {
    visit(node.body);
    return null;
  }
}
