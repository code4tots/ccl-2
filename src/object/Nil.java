package com.ccl.core;

public final class Nil extends Value {
  public static final Nil value = new Nil();

  public static final Blob META = new Blob(Blob.META)
      .setattr("name", Text.from("Nil"));
  
  private Nil() {}

  @Override
  public Blob getMeta() {
    return META;
  }
}
