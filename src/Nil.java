// javac -Xlint *.java && java -ea Nil

public class Nil extends Obj {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  public static final Obj NIL = new Nil();

  private Nil() {
    super(TYPE);
  }

  static {
    TYPE.setattr("__str__", new Function("nil.__str__") {
      public Obj call(Obj... args) {
        return X("nil");
      }
    });

    TYPE.setattr("__hash__", new Function("nil.__hash__") {
      public Obj call(Obj... args) {
        return X(0);
      }
    });

    TYPE.setattr("__eq__", new Function("nil.__eq__") {
      public Obj call(Obj... args) {
        return X(args[1] instanceof Nil);
      }
    });
  }

  public static void main(String[] args) {
    assert NIL.toString().equals("nil"): NIL.toString();
    assert NIL.equals(NIL);
  }
}
