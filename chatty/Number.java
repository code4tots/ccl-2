package com.chatty;

public final class Number extends Value {
  private static final Blob META = new Blob(Blob.META);

  private final Double value;
  public Number(Double value) { this.value = value; }
  public Blob getMeta() { return META; }
}
