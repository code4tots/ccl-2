import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

public final class UserFunc extends Func {

  public final Ast.Function ast;
  public final Scope scope;
  public UserFunc(Ast.Function ast, Scope scope) {
    this.ast = ast;
    this.scope = scope;
  }
  public final String getTraceMessage() {
    return "\nin user function defined in " + ast.token.getLocationString();
  }
  public final Val calli(Val self, ArrayList<Val> args) {
    Scope scope = this.scope;
    if (ast.newScope) {
      scope = new Scope(this.scope);
      scope.put("self", self);
    }
    if (ast.vararg == null) {
      if (ast.optargs.size() == 0)
        Err.expectArglen(args, ast.args.size());
      else
        Err.expectArgRange(
            args, ast.args.size(), ast.args.size() + ast.optargs.size());
    }
    else
      Err.expectMinArglen(args, args.size());

    Iterator<Val> vit = args.iterator();
    Iterator<String> sit = ast.args.iterator();

    while (sit.hasNext())
      scope.put(sit.next(), vit.next());

    sit = ast.optargs.iterator();

    while (sit.hasNext() && vit.hasNext())
      scope.put(sit.next(), vit.next());

    while (sit.hasNext())
      scope.put(sit.next(), Nil.val);

    if (ast.vararg != null) {
      ArrayList<Val> va = new ArrayList<Val>();
      while (vit.hasNext())
        va.add(vit.next());
      scope.put(ast.vararg, List.from(va));
    }

    return scope.eval(ast.body);
  }
}
