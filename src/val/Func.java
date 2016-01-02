import java.util.ArrayList;
import java.util.HashMap;

public abstract class Func extends Val implements Traceable {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Func"))
      .put(new BuiltinFunc("__call__") {
        public Val calli(Val self, ArrayList<Val> args) {
          return self.as(Func.class, "self").call(self, args);
        }
      })
      .put(new BuiltinFunc("apply") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 2);
          Val s = args.get(0);
          ArrayList<Val> va = args.get(1).as(List.class, "arg 1").val;
          return self.as(Func.class, "self").call(s, va);
        }
      })
      .hm;

  public final HashMap<String, Val> getMeta() { return MM; }
  public final Val call(Val self, ArrayList<Val> args) {
    try {
      Val result = calli(self, args);
      if (result == null)
        throw new Err("Function returned a Java null pointer!");
      return result;
    }
    catch (final Err e) { e.add(this); throw e; }
    catch (final Throwable e) { throw new Err(e); }
  }
  public abstract Val calli(Val self, ArrayList<Val> args);
}
