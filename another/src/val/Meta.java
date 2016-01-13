package com.ccl.val;

public final class Meta extends BaseBlob {
  public static final Meta META = new Meta("Meta");

  public final String name;
  public Meta(String name) { this.name = name; }
  public Meta getMeta() { return META; }
  public int hashCode() { return identityHash(); }
}
