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

  static {
    TYPE.setattr("__eq__", new Function("num.__eq__") {
      public Obj call(Obj... args) {
        return X(args[1] instanceof Num && args[0].toDouble().equals(args[1].toDouble()));
      }
    });
  }

  public static void main(String[] args) {
    assert X(1).equals(X(1));
  }
}
