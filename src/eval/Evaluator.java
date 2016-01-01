// Technically speaking, we could have Ast.Return appear in e.g.
// the condition of a while loop.
// This shouldn't normally happen given the way the Parser works.
// TODO: Code defensively against messed up AST.

import java.util.ArrayList;

public class Evaluator extends AstVisitor<Val> {
  public final Scope scope;
  public boolean ret = false, br = false, cont = false;

  public Evaluator(Scope scope) { this.scope = scope; }

  // Statement only

  public Val visitReturn(Ast.Return node) {
    Val val = visit(node.val);
    ret = true;
    return val;
  }

  public Val visitWhile(Ast.While node) {
    while (visit(node.cond).truthy()) {
      Val val = visit(node.body);
      if (ret) return val;
      if (br) break;
      if (cont) continue;
    }
    return Val.nil;
  }

  public Val visitBlock(Ast.Block node) {
    Val val = Val.nil;
    for (int i = 0; i < node.body.size(); i++) {
      val = visit(node.body.get(i));
      if (ret||br||cont) return val;
    }
    return val;
  }

  public Val visitBreak(Ast.Break node) {
    br = true;
    return null;
  }

  public Val visitContinue(Ast.Continue node) {
    cont = true;
    return null;
  }

  // Statement or Expression

  public Val visitIf(Ast.If node) {
    return visit(visit(node.cond).truthy() ? node.body : node.other);
  }

  // Expression only

  public Val visitNum(Ast.Num node) {
    return Num.from(node.val);
  }

  public Val visitStr(Ast.Str node) {
    return Val.Str.from(node.val);
  }

  public Val visitName(Ast.Name node) {
    Val val = scope.getOrNull(node.name);
    if (val == null) {
      Err e = new Err("Variable '" + node.name + "' not defined");
      e.add(node);
      throw e;
    }
    return val;
  }

  public Val visitAssign(Ast.Assign node) {
    Val val = visit(node.val);
    scope.put(node.name, val);
    return val;
  }

  public Val visitFunction(Ast.Function node) {
    return new UserFunc(
        node.token, node.args, node.vararg, node.body, scope);
  }

  public Val visitCall(Ast.Call node) {
    Val owner = visit(node.owner);
    ArrayList<Val> args = new ArrayList<Val>();
    for (int i = 0; i < node.args.size(); i++)
      args.add(visit(node.args.get(i)));
    if (node.vararg != null)
      args.addAll(visit(node.vararg).as(Val.List.class, "vararg").val);
    return (node.name.equals("__call__") && (owner instanceof Func)) ?
        ((Func) owner).call(owner, args):
        owner.call(node.name, args);
  }

  public Val visitGetMethod(Ast.GetMethod node) {
    // TODO: Figure out if this is really worth it.
    throw new Err("Not implemented");
  }

  public Val visitGetAttribute(Ast.GetAttribute node) {
    Val v = visit(node.owner).as(Val.Blob.class, "self").attrs.get(node.name);
    if (v == null)
      throw new Err(
          "No attribute '" + node.name + "' for type " + v.getMetaName());
    return v;
  }

  public Val visitSetAttribute(Ast.SetAttribute node) {
    Val v = visit(node.val);
    visit(node.owner).as(Val.Blob.class, "self").attrs.put(node.name, v);
    return v;
  }

  public Val visitIs(Ast.Is node) {
    return visit(node.left) == visit(node.right) ? Val.tru : Val.fal;
  }

  public Val visitIsNot(Ast.IsNot node) {
    return visit(node.left) != visit(node.right) ? Val.tru : Val.fal;
  }

  public Val visitNot(Ast.Not node) {
    return visit(node.target).truthy() ? Val.fal : Val.tru;
  }

  public Val visitAnd(Ast.And node) {
    Val left = visit(node.left);
    return left.truthy() ? visit(node.right) : left;
  }

  public Val visitOr(Ast.Or node) {
    Val left = visit(node.left);
    return left.truthy() ? left : visit(node.right);
  }

  // Module

  public Val visitModule(Ast.Module node) {
    return visit(node.body);
  }
}
