package com.ccl.core;

import java.util.Formatter;

public final class Text extends Value {

  public static Text from(String value) {
    return new Text(value);
  }

  public static final Blob META = new Blob(Blob.META)
      .setattr("__mod__", new BuiltinFunction("Text@__mod__") {
        @Override
        public Value calli(Value owner, List args) {
          List list = args.get(0).as(List.class);
          Object[] arr = new Object[list.size()];
          for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
          }
          return Text.from(new Formatter()
              .format(owner.as(Text.class).getValue(), arr)
              .toString());
        }
      });

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
