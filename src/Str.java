public class Str extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static Obj X(String s) { return new Str(s); }

  private final String value;

  private Str(String value) {
    super(TYPE);
    this.value = value;
  }

  public String toString() {
    return value;
  }
}
