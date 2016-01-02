import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

public final class BuiltinIter extends Val {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("BuiltinIter"))
      .put(new BuiltinFunc("BuiltinIter#more") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return Bool.from(self.as(BuiltinIter.class, "self").val.hasNext());
        }
      })
      .put(new BuiltinFunc("BuiltinIter#next") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return self.as(BuiltinIter.class, "self").val.next();
        }
      })
      .hm;

  public final HashMap<String, Val> getMeta() { return MM; }

  public final Iterator<Val> val;
  public BuiltinIter(Iterator<Val> it) { this.val = it; }
}
