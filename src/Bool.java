public class Bool extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static final Obj TRUE = new Bool(true);
  public static final Obj FALSE = new Bool(false);

  private final boolean value;

  private Bool(boolean value) {
    super(TYPE);
    this.value = value;
  }

  public boolean toBoolean() {
    return value;
  }
}
