import java.util.HashMap;

public final class Map extends Val.Wrap<HashMap<Val, Val>> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Map"))
      .hm;

  public static Map from(HashMap<Val, Val> s) { return new Map(s); }
  public Map(HashMap<Val, Val> val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MM; }
}
