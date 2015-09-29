abstract public class Callable extends Obj {

  public Callable(Obj type) {
    super(type);
  }

  abstract public Obj call(Obj... args);
}
