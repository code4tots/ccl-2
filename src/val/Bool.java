import java.util.HashMap;

public final class Bool extends Val.Wrap<Boolean> {

  public static final HashMap<String, Val> MMBool = new Hmb()
      .put("name", Str.from("Bool"))
      .hm;

  public static final Bool tru = new Bool(true);
  public static final Bool fal = new Bool(false);

  private Bool(Boolean val) { super(val); }
  public final HashMap<String, Val> getMeta() { return MMBool; }
}
