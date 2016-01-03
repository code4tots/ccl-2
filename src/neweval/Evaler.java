import java.util.ArrayList;
import java.util.Collections;

// Evaluator2
public class Evaler extends AstVisitor<Void> {

  public final Scope scope;
  private final ArrayList<Ast> todoStack = new ArrayList<Ast>();
  private final ArrayList<Integer> indexStack = new ArrayList<Integer>();
  private final ArrayList<Val> valStack = new ArrayList<Val>();
  public boolean ret = false, yld = false, br = false, cont = false;

  public Evaler(Scope scope) {
    this.scope = scope;
    push(Nil.val);
  }

  public Val resume() {

    while (!ret && !yld) {
      Ast node = todoStack.remove(todoStack.size()-1);
      // TODO: Clean this up. This feels a bit like a dirty hack because it
      // uses knowledge about which configuration is able to handle breaks and
      // continues.
      if (br||cont&&!(
          node instanceof Ast.While &&
          indexStack.get(indexStack.size()-1) == 2))
        continue;
      visit(node);
    }
    yld = false;
    if (todoStack.isEmpty())
      ret = true;
    return pop();
  }

  public void push(Ast node, Integer index) {
    todoStack.add(node);
    indexStack.add(index);
  }

  public void push(Val val) {
    valStack.add(val);
  }

  public Val pop() {
    return valStack.remove(valStack.size()-1);
  }

  public Val peek() {
    return valStack.get(valStack.size()-1);
  }

  // Every statement is expected to start with valStack.size() == 1 and
  // end with valStack.size() == 1.
  public void startStatement() {
    if (valStack.size() != 1)
      throw new Err("FUBAR " + valStack.size());
    pop();
  }

  // Statement only
  public Void visitReturn(Ast.Return node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0: // Queue up: return(1) and expression to return.
      startStatement();
      push(node, 1);
      push(node.val, 0);
      break;
    case 1: // Return the value on the stack.
      ret = true;
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitWhile(Ast.While node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0: // Queue up: while(1) and condition to branch on.
      startStatement();
      push(node, 1);
      push(node.cond, 0);
      break;
    case 1: // Check evaluated condition, and only loop if true.
      if (pop().truthy()) {
        push(node, 2);
        push(node.body, 0);
      } else {
        push(Nil.val);
      }
      break;
    case 2: // handle break and continue and start over if appropriate.
      if (br)
        br = false;
      else {
        cont = false;
        push(node, 0);
      }
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitBlock(Ast.Block node) {
    int index = indexStack.remove(indexStack.size()-1);
    if (index < node.body.size()) {
      push(node, index+1);
      push(node.body.get(index), 0);
    }
    return null;
  }
  public Void visitBreak(Ast.Break node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      br = true;
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitContinue(Ast.Continue node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      cont = true;
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }

  // Statement or Expression
  public Void visitIf(Ast.If node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.cond, 0);
      break;
    case 1:
      if (pop().truthy())
        push(node.body, 0);
      else
        push(node.other, 1);
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }

  // Expression only
  public Void visitNum(Ast.Num node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(Num.from(node.val));
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitStr(Ast.Str node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(Str.from(node.val));
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitName(Ast.Name node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      Val val = scope.getOrNull(node.name);
      if (val == null) {
        Err e = new Err("Variable '" + node.name + "' not defined");
        e.add(node);
        throw e;
      }
      push(val);
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitAssign(Ast.Assign node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.val, 0);
      break;
    case 1:
      scope.put(node.name, peek());
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitFunction(Ast.Function node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(new UserFunc(
        node.token, node.args, node.optargs, node.vararg, node.body, scope));
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitCall(Ast.Call node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      if (node.vararg != null)
        push(node.vararg, 0);
      for (int i = node.args.size()-1; i >= 0; i--)
        push(node.args.get(i), 0);
      push(node.owner, 0);
      break;
    case 1:
      ArrayList<Val> args = new ArrayList<Val>();
      if (node.vararg != null) {
        args.addAll(pop().as(List.class, "vararg").val);
        Collections.reverse(args);
      }
      for (int i = 0; i < node.args.size(); i++)
        args.add(pop());
      Collections.reverse(args);
      Val owner = pop();

      try {
        if (node.name.equals("__call__") && owner instanceof Func)
          push(((Func) owner).call(owner, args));
        else
          push(owner.call(node.name, args));
      }
      catch (final Err e) { e.add(node); throw e; }
      catch (final Throwable e) { throw new Err(e); }
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitGetMethod(Ast.GetMethod node) {
    try { throw new Err("GetMethod not implemented"); }
    catch (final Err e) { e.add(node); throw e; }
  }
  public Void visitGetAttribute(Ast.GetAttribute node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.owner, 0);
      break;
    case 1:
      Val owner = pop();
      Val v = owner.as(Blob.class, "self").attrs.get(node.name);
      if (v == null)
        throw new Err(
            "No attribute '" + node.name + "' for type " + owner.getMetaName());
      push(v);
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitSetAttribute(Ast.SetAttribute node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.val, 0);
      push(node.owner, 0);
      break;
    default:
      Val owner = pop();
      owner.as(Blob.class, "self").attrs.put(node.name, peek());
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitIs(Ast.Is node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.right, 0);
      push(node.left, 0);
      break;
    case 1:
      push(Bool.from(pop() == pop()));
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitIsNot(Ast.IsNot node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.right, 0);
      push(node.left, 0);
      break;
    case 1:
      push(Bool.from(pop() != pop()));
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitNot(Ast.Not node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.target, 0);
      break;
    case 1:
      push(Bool.from(!pop().truthy()));
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitAnd(Ast.And node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.left, 0);
      break;
    case 1:
      if (peek().truthy()) {
        pop();
        push(node.right, 0);
      }
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }
  public Void visitOr(Ast.Or node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      push(node, 1);
      push(node.left, 0);
      break;
    case 1:
      if (!peek().truthy()) {
        pop();
        push(node.right, 0);
      }
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }

  // Module
  public Void visitModule(Ast.Module node) {
    int index = indexStack.remove(indexStack.size()-1);
    switch (index) {
    case 0:
      startStatement();
      push(node.body, 0);
      break;
    default:
      throw new Err("FUBAR: " + index);
    }
    return null;
  }

}
