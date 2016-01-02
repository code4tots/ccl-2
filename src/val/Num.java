import java.util.ArrayList;
import java.util.HashMap;

public final class Num extends Val.Wrap<Double> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Num"))
      .put(new BuiltinFunc("repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          double val = self.as(Num.class, "self").val;
          return Str.from(Math.floor(val) == val ?
              Integer.toString((int) val) :
              Double.toString(val));
        }
      })
      .put(new BuiltinFunc("__add__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return Num.from(
              self.as(Num.class, "self").val +
              args.get(0).as(Num.class, "argument").val);
        }
      })
      .put(new BuiltinFunc("__sub__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return Num.from(
              self.as(Num.class, "self").val -
              args.get(0).as(Num.class, "argument").val);
        }
      })
      .put(new BuiltinFunc("__eq__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          if (args.get(0) instanceof Num)
            return Bool.from(
                self.as(Num.class, "self").val.equals(
                    ((Num)args.get(0)).val));
          return Bool.fal;
        }
      })
      .put(new BuiltinFunc("__lt__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return Bool.from(
              self.as(Num.class, "self").val.intValue() <
                  args.get(0).as(Num.class, "argument").val.intValue());
        }
      })
      .put(new BuiltinFunc("repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          double val = self.as(Num.class, "self").val;
          return Str.from(Math.floor(val) == val ?
              Integer.toString((int) val):
              Double.toString(val));
        }
      })
      .put(new BuiltinFunc("hash") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Num.from(self.as(Num.class, "self").val.hashCode());
        }
      })
      .hm;

  public static Num from(Double s) { return new Num(s); }
  public static Num from(Integer s) { return new Num(s.doubleValue()); }
  public Num(Double val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
