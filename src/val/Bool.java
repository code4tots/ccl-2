import java.util.HashMap;

public final class Bool extends Val.Wrap<Boolean> {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Bool"))
      .hm;

  public static final Bool tru = new Bool(true);
  public static final Bool fal = new Bool(false);

  private Bool(Boolean val) { super(val); }
  public static Bool from(Boolean val) { return val ? tru : fal; }
  public final HashMap<String, Val> getMeta() { return MM; }
}
