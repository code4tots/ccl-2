import java.util.ArrayList;

public class List extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static Obj A(Obj... args) { return new List(args); }

  private final ArrayList<Obj> value = new ArrayList<Obj>();

  private List(Obj... args) {
    super(TYPE);
    for (int i = 0; i < args.length; i++)
      value.add(args[i]);
  }

  public static void main(String[] args) {
  }
}
