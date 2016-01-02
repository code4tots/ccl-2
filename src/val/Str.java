import java.util.HashMap;
import java.util.ArrayList;

public final class Str extends Val.Wrap<String> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Str"))
      .put(new BuiltinFunc("__eq__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return
              self.as(Str.class, "self").val.equals(
                args.get(0).as(Str.class, "argument").val) ?
              Bool.tru : Bool.fal;
        }
      })
      .hm;

  public static Str from(String s) { return new Str(s); }
  public Str(String val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
