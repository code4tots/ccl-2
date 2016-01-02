import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

public final class List extends Val.Wrap<ArrayList<Val>> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("List"))
      .put(new BuiltinFunc("repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          StringBuilder sb = new StringBuilder("L[");
          Iterator<Val> it = self.as(List.class, "self").val.iterator();
          if (it.hasNext())
            sb.append(it.next().repr());
          while (it.hasNext())
            sb.append(", " + it.next().repr());
          sb.append("]");
          return Str.from(sb.toString());
        }
      })
      .put(new BuiltinFunc("len") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Num.from(self.as(List.class, "self").val.size());
        }
      })
      .put(new BuiltinFunc("__call__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(List.class, "self").val.get(
              args.get(0).as(Num.class, "index").val.intValue());
        }
      })
      .put(new BuiltinFunc("__setitem__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 2);
          return self.as(List.class, "self").val.set(
              args.get(0).as(Num.class, "index").val.intValue(),
              args.get(1));
        }
      })
      .put(new BuiltinFunc("add") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          self.as(List.class, "self").val.add(args.get(0));
          return self;
        }
      })
      .put(new BuiltinFunc("__eq__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          self.as(List.class, "self").val.equals(
              args.get(0).as(List.class, "arg").val);
          return self;
        }
      })
      .hm;

  public static List from(ArrayList<Val> s) { return new List(s); }

  public List(ArrayList<Val> val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
