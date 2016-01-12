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
    Evaluator.assign(scope, ast.args, List.from(args));

    return scope.eval(ast.body);
  }
}
