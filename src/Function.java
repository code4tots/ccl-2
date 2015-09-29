abstract public class Function extends Callable {

  public static final Obj TYPE = new Obj(TYPE_TYPE);

  // Whereas user defined functions can be identified by its location in code,
  // it's not always possible to generate the location for native code.
  // (Furthermore, even if you could, as a general rule, I don't want to
  // expose the implementation language to the CCL user)
  // As such, I think giving all native functions a name is a helpful practice.
  public Function(String name) {
    super(TYPE);
    setattr("__name__", X(name));
  }

  abstract public Obj call(Obj... args);
}
