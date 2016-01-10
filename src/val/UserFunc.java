import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

public final class UserFunc extends Func {

  public final Token token;
  public final ArrayList<String> args;
  public final ArrayList<String> optargs;
  public final String vararg;
  public final Ast body;
  public final boolean newScope;
  public final Scope scope;
  public UserFunc(
      Token token, ArrayList<String> args, ArrayList<String> optargs,
      String vararg, Ast body, boolean newScope, Scope scope) {
    this.token = token;
    this.args = args;
    this.optargs = optargs;
    this.vararg = vararg;
    this.body = body;
    this.newScope = newScope;
    this.scope = scope;
  }
  public final String getTraceMessage() {
    return "\nin user function defined in " + token.getLocationString();
  }
  public final Val calli(Val self, ArrayList<Val> args) {
    Scope scope = this.scope;
    if (newScope) {
      scope = new Scope(this.scope);
      scope.put("self", self);
    }
    if (vararg == null) {
      if (optargs.size() == 0)
        Err.expectArglen(args, args.size());
      else
        Err.expectArgRange(args, args.size(), args.size() + optargs.size());
    }
    else
      Err.expectMinArglen(args, args.size());

    Iterator<Val> vit = args.iterator();
    Iterator<String> sit = this.args.iterator();

    while (sit.hasNext())
      scope.put(sit.next(), vit.next());

    sit = optargs.iterator();

    while (sit.hasNext() && vit.hasNext())
      scope.put(sit.next(), vit.next());

    while (sit.hasNext())
      scope.put(sit.next(), Nil.val);

    if (vararg != null) {
      ArrayList<Val> va = new ArrayList<Val>();
      while (vit.hasNext())
        va.add(vit.next());
      scope.put(vararg, List.from(va));
    }

    return scope.eval(body);
  }
}
