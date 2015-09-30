abstract public class Xccl {

  public final Obj MAIN_MODULE_NAME;
  public final Obj CODE_REGISTRY = Obj.D();

  public Xccl(Obj main) {
    MAIN_MODULE_NAME = main;
  }

  public Obj getRootContext() {
    return Obj.D();
  }

  public void run() {
    Obj.eval(getRootContext(), CODE_REGISTRY.m("__getitem__", MAIN_MODULE_NAME));
  }

}
