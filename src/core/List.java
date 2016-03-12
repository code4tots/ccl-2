package com.ccl.core;

import java.util.ArrayList;

public final class List extends Value {
  
  public static final Blob META = new Blob(Blob.META);

  @Override
  public Blob getMeta() {
    return META;
  }

  public static List from(Value... value) {
    ArrayList<Value> list = new ArrayList<Value>();
    for (int i = 0; i < value.length; i++) {
      list.add(value[i]);
    }
    return new List(list);
  }

  public static List from(ArrayList<Value> value) {
    return new List(value);
  }

  private final ArrayList<Value> value;

  private List(ArrayList<Value> value) {
    this.value = value;
  }

  protected ArrayList<Value> getValue() {
    return value;
  }

  public Value get(int i) {
    return value.get(i);
  }

  public int size() {
    return value.size();
  }
}
