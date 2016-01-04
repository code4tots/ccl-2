import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

public final class Map extends Val.Wrap<HashMap<Val, Val>> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Map"))
      .put(new BuiltinFunc("Map#hash") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Num.from(self.as(Map.class, "self").val.hashCode());
        }
      })
      .put(new BuiltinFunc("Map#repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          StringBuilder sb = new StringBuilder("M[");
          Iterator<HashMap.Entry<Val, Val>> it =
              self.as(Map.class, "self").val.entrySet().iterator();
          if (it.hasNext()) {
            HashMap.Entry<Val, Val> e = it.next();
            sb.append(e.getKey().repr());
            sb.append(", " + e.getValue().repr());
          }
          while (it.hasNext()) {
            HashMap.Entry<Val, Val> e = it.next();
            sb.append(", " + e.getKey().repr());
            sb.append(", " + e.getValue().repr());
          }
          sb.append("]");
          return Str.from(sb.toString());
        }
      })
      .put(new BuiltinFunc("Map#__call__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          Val result = self.as(Map.class, "self").val.get(args.get(0));
          if (result == null)
            throw new Err("Key " + args.get(0).repr() + " not found");
          return result;
        }
      })
      .put(new BuiltinFunc("Map#__setitem__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 2);
          return self.as(Map.class, "self").val.put(
              args.get(0), args.get(1));
        }
      })
      .put(new BuiltinFunc("Map#__eq__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return args.get(0) instanceof Map ?
              Bool.from(self.as(Map.class, "self").val.equals(
                ((Map) args.get(0)).val)):
              Bool.fal;
        }
      })
      .put(new BuiltinFunc("Map#iter") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          final Iterator<HashMap.Entry<Val, Val>> it =
              self.as(Map.class, "self").val.entrySet().iterator();
          return new BuiltinIter(new Iterator<Val>() {
            public boolean hasNext() {
              return it.hasNext();
            }
            public Val next() {
              HashMap.Entry<Val, Val> e = it.next();
              return List.from(toArrayList(e.getKey(), e.getValue()));
            }
          });
        }
      })
      .hm;

  public static Map from(HashMap<Val, Val> s) { return new Map(s); }
  private Map(HashMap<Val, Val> val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
