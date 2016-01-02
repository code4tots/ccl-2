import java.util.HashMap;
import java.util.ArrayList;

public final class Str extends Val.Wrap<String> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Str"))
      .put(new BuiltinFunc("Str#hash") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Num.from(self.as(Str.class, "self").val.hashCode());
        }
      })
      .put(new BuiltinFunc("__eq__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return
              self.as(Str.class, "self").val.equals(
                args.get(0).as(Str.class, "argument").val) ?
              Bool.tru : Bool.fal;
        }
      })
      .put(new BuiltinFunc("__add__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return Str.from(
              self.as(Str.class, "self").val +
              args.get(0).as(Str.class, "argument").val);
        }
      })
      .put(new BuiltinFunc("repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          String s = self.as(Str.class, "self").val;
          StringBuilder sb = new StringBuilder("\""); // TODO: Be more thorough
          for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\"': sb.append("\\\""); break;
            case '\\': sb.append("\\\\"); break;
            default: sb.append(c);
            }
          }
          sb.append("\"");
          return Str.from(sb.toString());
        }
      })
      .hm;

  public static Str from(String s) { return new Str(s); }
  public Str(String val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
