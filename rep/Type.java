public class Type extends Obj {

  public final Obj TYPE = new Type();

  private Type() {}

  public Type(Obj bases...) {
    super(TYPE);
  }

}
