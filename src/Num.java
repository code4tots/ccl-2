public class Num extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static Obj X(Double value) { return new Num(value); }
  public static Obj X(Integer value) { return new Num(value.doubleValue()); }

  public final Double value;

  private Num(Double value) {
    super(TYPE);
    this.value = value;
  }

  public Double toDouble() {
    return value;
  }
}
