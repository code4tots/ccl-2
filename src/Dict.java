import java.util.HashMap;

public class Dict extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static Obj D(Obj... args) { return new Dict(args); }

  private final HashMap<Obj, Obj> value = new HashMap<Obj, Obj>();

  private Dict(Obj... args) {
    super(TYPE);
    if (args.length % 2 != 0)
      throw err("Tried to construct dict with odd number of arguments");

    for (int i = 0; i < args.length; i+=2)
      value.put(args[i], args[i+1]);
  }

  public static void main(String[] args) {
  }
}
