package com.ccl.core;

public final class Text extends Value {

  public static Text from(String value) {
    return new Text(value);
  }

  public static final Blob META = new Blob(Blob.META)
      .setattr("name", Text.from("Text"))
      .setattr("str", new BuiltinFunction("Text#str") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 0);
          return owner.as(Text.class);
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
