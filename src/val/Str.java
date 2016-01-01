import java.util.HashMap;

public final class Str extends Val.Wrap<String> {

  public static final HashMap<String, Val> MMStr = new Hmb()
      .put("name", Str.from("Str"))
      .hm;

  public static Str from(String s) { return new Str(s); }
  public Str(String val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MMStr; }
}
