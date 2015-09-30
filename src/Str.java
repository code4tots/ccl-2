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

  static {
    TYPE.setattr("__str__", new Function("str.__str__") {
      public Obj call(Obj... args) {
        return args[0];
      }
    });

    TYPE.setattr("__hash__", new Function("str.__hash__") {
      public Obj call(Obj... args) {
        return X(args[0].toString().hashCode());
      }
    });

    TYPE.setattr("__eq__", new Function("str.__eq__") {
      public Obj call(Obj... args) {
        return X(args[1] instanceof Str && args[0].toString().equals(args[1].toString()));
      }
    });
  }

  public static void main(String[] args) {
  }
}
