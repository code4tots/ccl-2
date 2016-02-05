package com.chatty;

public final class Text extends Value {
  private static final Blob META = new Blob(Blob.META);

  private final String value;
  public Text(String value) {
    this.value = value;
  }
  public Blob getMeta() { return META; }
}
