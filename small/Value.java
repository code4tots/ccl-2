package com.ccl.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class Value {
  public static final Nil nil = Nil.nil;
  public static final Bool tru = Bool.tru;
  public static final Bool fal = Bool.fal;
  public Blob getMeta() { return null; }
  public Value call(Value owner, List args) { throw new Err("Not callable"); }
  public final Value call(Value owner, java.lang.String name, List args) {
    return owner.getMeta().get(name).call(owner, args);
  }
  public boolean truthy() { return this != nil && this != fal; }
  public static final class Nil extends Value {
    public static final Nil nil = new Nil();
    private Nil() {}
  }
  public static final class Bool extends Value {
    public static final Bool tru = new Bool();
    public static final Bool fal = new Bool();
    private Bool() {}
  }
  public static final class Int extends Value {
    public static Int from(BigInteger value) { return new Int(value); }
    public final BigInteger value;
    private Int(BigInteger value) { this.value = value; }
    public int hashCode() { return value.hashCode(); }
  }
  public static final class Float extends Value {
    public static Float from(Double value) { return new Float(value); }
    public final Double value;
    public Float(Double value) { this.value = value; }
    public int hashCode() { return value.hashCode(); }
  }
  public static final class String extends Value {
    public static String from(java.lang.String value) {
      return new String(value);
    }
    public final java.lang.String value;
    private String(java.lang.String value) { this.value = value; }
    public int hashCode() { return value.hashCode(); }
  }
  public static final class List extends Value {
    public static List from(ArrayList<Value> value) {
      return new List(value);
    }
    public static List from(Value... value) {
      ArrayList<Value> list = new ArrayList<Value>();
      for (int i = 0; i < value.length; i++)
        list.add(value[i]);
      return new List(list);
    }
    public final ArrayList<Value> value;
    private List(ArrayList<Value> value) { this.value = value; }
    public int hashCode() { return value.hashCode(); }
  }
  public static final class Map extends Value {
    public final HashMap<Value, Value> value;
    private Map(HashMap<Value, Value> value) { this.value = value; }
    public int hashCode() { return value.hashCode(); }
  }
  public static final class Function extends Value {
    private final Ast.Function ast;
    private final Scope scope;
    public Function(Ast.Function ast, Scope scope) {
      this.ast = ast;
      this.scope = scope;
    }
    public Value call(Value owner, List args) {
      Scope scope = ast.newScope ? new Scope(this.scope) : this.scope;
      if (ast.newScope)
        scope.put("self", owner);
      ast.args.assign(scope, args);
      Context ctx = new Context(scope);
      ast.body.run(ctx);
      return ctx.value;
    }
  }
  public static final class Blob extends Value {
    private final HashMap<java.lang.String, Value> table =
        new HashMap<java.lang.String, Value>();
    public Value getOrNull(java.lang.String key) { return table.get(key); }
    public Value get(java.lang.String key) {
      Value value = getOrNull(key);
      if (value == null)
        throw new Err("No attribute named: " + key);
      return value;
    }
    public int hashCode() {
      Value func = getMeta().getOrNull("__hash__");
      if (func == null)
        return super.hashCode();
      return func.call(this, List.from()).hashCode();
    }
  }
  // API classes.
  public abstract static class BuiltinFunction extends Value {
    public abstract Value call(Value owner, List args);
  }
  public abstract static class BuiltinValue<T> extends Value {
    public final T value;
    public BuiltinValue(T value) { this.value = value; }
  }
}
