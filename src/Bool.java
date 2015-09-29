public class Bool extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static final Obj TRUE = new Bool(true);
  public static final Obj FALSE = new Bool(false);

  private final Boolean value;

  private Bool(Boolean value) {
    super(TYPE);
    this.value = value;
  }

  public Boolean toBoolean() {
    return value;
  }

  static {
    TYPE.setattr("__hash__", new Function("bool.__hash__") {
      public Obj call(Obj... args) {
        return X(args[0].toBoolean().hashCode());
      }
    });
  }

  public static void main(String[] args) {
  }
}
