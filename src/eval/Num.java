import java.util.HashMap;

public final class Num extends Val.Wrap<Double> {

  public static final HashMap<String, Val> MMNum = new Hmb()
      .put("name", Str.from("Num"))
      .hm;

  public static Num from(Double s) { return new Num(s); }
  public Num(Double val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MMNum; }
}
