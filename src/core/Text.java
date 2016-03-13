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
          ErrUtils.expectArglen(args, 1);
          List list = args.get(0).as(List.class);
          Object[] arr = new Object[list.size()];
          for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
          }
          return Text.from(new Formatter()
              .format(owner.as(Text.class).getValue(), arr)
              .toString());
        }
      })
      .setattr("__eq__", new BuiltinFunction("Text@__eq__") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 1);
          if (args.get(0) instanceof Text)
            return owner.as(Text.class).value.equals(
                ((Text) args.get(0)).value) ? Bool.yes : Bool.no;
          return Bool.no;
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
