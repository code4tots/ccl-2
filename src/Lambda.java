/*
Lambda probably doesn't have to be a separate class.
But it's convenient right now to make it so.
*/
import java.util.ArrayList;

// WARNING: I should probably implement Obj.eval before using this.
public class Lambda extends Callable {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public final Obj context, node;

  public Lambda(Obj context, Obj node) {
    super(TYPE);
    this.context = context;
    this.node = node;
  }

  public Obj call(Obj... args) {
    Obj ctx = D(X("__parent__"), context);

    ArrayList<Obj> argnames = node.m("__getitem__", X("args")).toArrayList();

    // TODO: varargs
    // TODO: arg checks.

    Obj body = node.m("__getitem__", X("body"));

    for (int i = 0; i < args.length; i++)
      ctx.attrs.put(argnames.get(i).toString(), args[i]);

    return eval(ctx, body);
  }
}
