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
      .hm;

  public final HashMap<String, Val> getMeta() { return MM; }
  public abstract Val call(Val self, ArrayList<Val> args);
}
