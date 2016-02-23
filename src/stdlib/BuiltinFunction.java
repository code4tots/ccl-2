package com.ccl.core;

public abstract class BuiltinFunction extends Function implements Traceable {
  public static final Blob META = new Blob(Blob.META)
      .setattr("name", Text.from("BuiltinFunction"))
      .setattr("__str__", new BuiltinFunction("BuiltinFunction#__str__") {
        @Override
        public Value calli(Value owner, List args) {
          ErrUtils.expectArglen(args, 0);
          return Text.from(owner.as(BuiltinFunction.class).name);
        }
      });

  public final String name;
  public BuiltinFunction(String name) {
    this.name = name;
  }

  public abstract Value calli(Value owner, List args);

  @Override
  public Blob getMeta() {
    return META;
  }

  @Override
  public final Value call(Value owner, List args) {
    try {
      Value result = calli(owner, args);
      if (result == null) {
        throw new Err("Builtin function returned null");
      }
      return result;
    } catch (final Err e) {
      e.add(this);
      throw e;
    } catch (final Throwable e) {
      throw new Err(e, this);
    }
  }

  @Override
  public final String getTraceMessage() {
    return "\nin builtin function '" + name + "'";
  }
}
