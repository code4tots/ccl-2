package com.ccl.val;

import java.util.ArrayList;

public final class Blob extends BaseBlob {
  private final Meta meta;
  public Blob(Meta meta) { this.meta = meta; }
  public Meta getMeta() { return meta; }
  public int hashCode() {
    return meta.has("hash") ?
      call("hash").as(Int.class, "hash").asIndex():
      identityHash();
  }
}
