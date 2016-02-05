package com.chatty;

import java.util.ArrayList;

public final class List extends Value {
  private static final Blob META = new Blob(Blob.META);

  private final ArrayList<Value> value;
  public List(ArrayList<Value> value) { this.value = value; }
  public List(Value... value) {
    this(new ArrayList<Value>());
    for (int i = 0; i < value.length; i++)
      this.value.add(value[i]);
  }
  public Blob getMeta() { return META; }
  public void add(Value item) { value.add(item); }
  public Value get(int i) { return value.get(i); }
  public int size() { return value.size(); }
}
