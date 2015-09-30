/*
Method probably doesn't have to be a separate class.
But it's convenient right now to make it so.
*/
public class Method extends Callable {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public final Obj self, callable;

  public Method(Obj self, Obj callable) {
    super(TYPE);
    this.self = self;
    this.callable = callable;
  }

  public Obj call(Obj... args) {
    Obj[] newargs = new Obj[args.length+1];

    newargs[0] = self;
    for (int i = 0; i < args.length; i++)
      newargs[i+1] = args[i];

    return callable.call(newargs);
  }

  public static void main(String[] args) {
  }
}
