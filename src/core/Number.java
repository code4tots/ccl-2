package com.ccl.core;

public final class Number extends Value {

  public static Number from(double value) {
    return new Number(value);
  }

  public static final Blob META = new Blob(Blob.META)
      .setattr("__lt__", new BuiltinFunction("Number@__lt__") {
        @Override
        public Value calli(Value owner, List args) {
          return
              owner.as(Number.class).getValue() <
              args.get(0).as(Number.class).getValue() ? Bool.yes : Bool.no;
        }
      })
      .setattr("__eq__", new BuiltinFunction("Number@__eq__") {
        @Override
        public Value calli(Value owner, List args) {
          Value other = args.get(0);
          return
              other instanceof Number &&
              owner.as(Number.class).getValue() ==
              other.as(Number.class).getValue() ? Bool.yes : Bool.no;
        }
      })
      .setattr("__add__", new BuiltinFunction("Number@__add__") {
        @Override
        public Value calli(Value owner, List args) {
          return Number.from(
              owner.as(Number.class).getValue() +
              args.get(0).as(Number.class).getValue());
        }
      })
      .setattr("__sub__", new BuiltinFunction("Number@__sub__") {
        @Override
        public Value calli(Value owner, List args) {
          return Number.from(
              owner.as(Number.class).getValue() -
              args.get(0).as(Number.class).getValue());
        }
      })
      .setattr("__mul__", new BuiltinFunction("Number@__mul__") {
        @Override
        public Value calli(Value owner, List args) {
          return Number.from(
              owner.as(Number.class).getValue() *
              args.get(0).as(Number.class).getValue());
        }
      })
      .setattr("__div__", new BuiltinFunction("Number@__div__") {
        @Override
        public Value calli(Value owner, List args) {
          return Number.from(
              owner.as(Number.class).getValue() /
              args.get(0).as(Number.class).getValue());
        }
      })
      .setattr("__mod__", new BuiltinFunction("Number@__mod__") {
        @Override
        public Value calli(Value owner, List args) {
          return Number.from(
              owner.as(Number.class).getValue() %
              args.get(0).as(Number.class).getValue());
        }
      })
      .setattr("repr", new BuiltinFunction("Number@repr") {
        @Override
        public Value calli(Value owner, List args) {
          return Text.from(Double.toString(owner.as(Number.class).getValue()));
        }
      });

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
