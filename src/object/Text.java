package com.ccl.core;

public final class Text extends Value {

  public static Text from(String value) {
    return new Text(value);
  }

  public static final Blob META = new Blob(Blob.META);

  private final String value;

  public Text(String value) {
    this.value = value;
  }

  @Override
  public Blob getMeta() {
    return META;
  }

  public String getValue() {
    return value;
  }
}
