package com.ccl.core;

public final class Number extends Value {

  public static Number from(double value) {
    return new Number(value);
  }

  public static final Blob META = new Blob(Blob.META);

  private final double value;

  private Number(double value) {
    this.value = value;
  }
  
  @Override
  public Blob getMeta() {
    return META;
  }

  public double getValue() {
    return value;
  }
}
