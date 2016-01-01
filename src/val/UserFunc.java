import java.util.ArrayList;
import java.util.HashMap;

public final class UserFunc extends Func {

  public final Token token;
  public final ArrayList<String> args;
  public final String vararg;
  public final Ast body;
  public final Scope scope;
  public UserFunc(
      Token token, ArrayList<String> args, String vararg,
      Ast body, Scope scope) {
    this.token = token;
    this.args = args;
    this.vararg = vararg;
    this.body = body;
    this.scope = scope;
  }
  public final String getTraceMessage() {
    return "\nin user function defined in " + token.getLocationString();
  }
  public final Val call(Val self, ArrayList<Val> args) {
    Scope scope = new Scope(this.scope);
    if (vararg == null)
      Err.expectArglen(args, args.size());
    else
      Err.expectMinArglen(args, args.size());

    for (int i = 0; i < this.args.size(); i++)
      scope.put(this.args.get(i), args.get(i));

    if (vararg != null) {
      ArrayList<Val> va = new ArrayList<Val>();
      for (int i = this.args.size(); i < args.size(); i++)
        va.add(args.get(i));
      scope.put(vararg, List.from(va));
    }

    return scope.eval(body);
  }
}
