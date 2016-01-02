import java.util.HashMap;

public final class Blob extends Val {
  public final HashMap<String, Val> meta;
  public final HashMap<String, Val> attrs;
  public Blob(Blob meta) {
    this(meta.attrs);
  }
  public Blob(HashMap<String, Val> meta) {
    this(meta, new HashMap<String, Val>());
  }
  public Blob(HashMap<String, Val> meta, HashMap<String, Val> attrs) {
    this.meta = meta;
    this.attrs = attrs;
  }
  public final HashMap<String, Val> getMeta() { return meta; }
}
