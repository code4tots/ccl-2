package com.ccl;

import java.util.HashMap;

public class Runtime {

public static final Blob META_META = Blob.META_META;
public static final Blob META_MODULE = Blob.from(META_META);
public static final Blob META_NIL = Blob.from(META_META);
public static final Blob META_STRING = Blob.from(META_META);

public abstract static class Value {
  public abstract Blob getMeta();
}

public static final class String extends Value {
  public static String from(java.lang.String s) { return new String(s); }

  private final java.lang.String value;
  public String(java.lang.String s) { value = s; }
  @Override public Blob getMeta() { return META_STRING; }
}

public static final class Blob extends Value {
  public static final Blob META_META = new Blob();

  public static Blob from(Blob meta) { return new Blob(meta); }
  public static Blob from(Blob meta, java.lang.String name) {
    Blob blob = new Blob(meta);
    blob.attrs.put("__name__", String.from(name));
    return blob;
  }

  private final Blob meta;
  private final HashMap<java.lang.String, Value> attrs =
    new HashMap<java.lang.String, Value>();

  private Blob() { meta = this; }
  private Blob(Blob meta) { this.meta = meta; }

  @Override public Blob getMeta() { return meta; }
}

public static Blob newModuleScope() {
  return new Blob();
}

}
