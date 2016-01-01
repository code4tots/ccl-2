import java.util.ArrayList;
import java.util.HashMap;

public final class Num extends Val.Wrap<Double> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Num"))
      .put(new BuiltinFunc("repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          double val = self.as(Num.class, "self").val;
          return Str.from(Math.floor(val) == val ?
              Integer.toString((int) val) :
              Double.toString(val));
        }
      })
      .hm;

  public static Num from(Double s) { return new Num(s); }
  public Num(Double val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
