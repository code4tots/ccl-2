package com.ccl.core;

public final class Bool extends Value {
  public static final Bool yes = new Bool(true);
  public static final Bool no = new Bool(false);

  public static final Blob META = new Blob(Blob.META);

  @Override
  public Blob getMeta() {
    return META;
  }

  private final boolean value;

  private Bool(boolean value) {
    this.value = value;
  }

  public boolean getValue() {
    return value;
  }
}
