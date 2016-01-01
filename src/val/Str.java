import java.util.HashMap;
import java.util.ArrayList;

public final class Str extends Val.Wrap<String> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Str"))
      .put(new BuiltinFunc("str") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return self;
        }
      })
      .hm;

  public static Str from(String s) { return new Str(s); }
  public Str(String val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
