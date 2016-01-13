package com.ccl.val;

import com.ccl.err.Err;
import java.util.Iterator;

public abstract class Val {
  public abstract Meta getMeta();
  public abstract int hashCode();
  protected final int identityHash() { return super.hashCode(); }
  public final String getMetaName() { return getMeta().name; }
  public final Val call(String methodName, List args) {
    return getMeta()
      .get(methodName)
      .as(Func.class, "method")
      .call(this, args);
  }
  public Val call(Val self, List args) {
    throw new Err(
        "Calling value of type " + getMetaName() + " not supported");
  }
  public final Val call(String methodName, Val... args) {
    return call(methodName, List.from(args));
  }
  public final <T extends Val> T as(Class<T> cls, String name) {
    try { return cls.cast(this); }
    catch (final ClassCastException e) { throw new Err(e); }
  }
}
