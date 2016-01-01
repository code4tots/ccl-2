import java.util.ArrayList;
import java.util.HashMap;

public abstract class BuiltinFunc extends Func {

  public static final HashMap<String, Val> MMBuiltinFunc = new Hmb()
      .put("name", Str.from("BuiltinFunc"))
      .put("__call__", new BuiltinFunc("__call__") {
        public Val calli(Val self, ArrayList<Val> args) {
          return self.as(BuiltinFunc.class, "self").call(self, args);
        }
      })
      .hm;

  public final String name;
  public BuiltinFunc(String name) { this.name = name; }
  public final HashMap<String, Val> getMeta() { return MMBuiltinFunc; }
  public final String getTraceMessage() {
    return "\nin builtin function " + name;
  }
  public final Val call(Val self, ArrayList<Val> args) {
    try { return calli(self, args); }
    catch (final Err e) { e.add(this); throw e; }
    catch (final Exception e) { throw new Err(e); }
  }
  public abstract Val calli(Val self, ArrayList<Val> args);
}
