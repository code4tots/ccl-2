import java.util.ArrayList;
import java.util.HashMap;

public final class List extends Val.Wrap<ArrayList<Val>> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("List"))
      .hm;

  public static List from(ArrayList<Val> s) { return new List(s); }

  public List(ArrayList<Val> val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
