import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigInteger;

public abstract class Num extends Val {

  public static final HashMap<String, Val> MM = new Hmb()
      .put("name", Str.from("Num"))
      .put(new BuiltinFunc("Num#hash") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return self.as(Num.class, "self").hash();
        }
      })
      .put(new BuiltinFunc("Num#repr") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 0);
          return self.as(Num.class, "self").numrepr();
        }
      })
      .put(new BuiltinFunc("Num#__add__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(Num.class, "self").add(
              args.get(0).as(Num.class, "argument"));
        }
      })
      .put(new BuiltinFunc("Num#__sub__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(Num.class, "self").sub(
              args.get(0).as(Num.class, "argument"));
        }
      })
      .put(new BuiltinFunc("Num#__mul__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(Num.class, "self").mul(
              args.get(0).as(Num.class, "argument"));
        }
      })
      .put(new BuiltinFunc("Num#__div__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(Num.class, "self").div(
              args.get(0).as(Num.class, "argument"));
        }
      })
      .put(new BuiltinFunc("Num#__mod__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(Num.class, "self").mod(
              args.get(0).as(Num.class, "argument"));
        }
      })
      .put(new BuiltinFunc("Num#__eq__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          Val v = args.get(0);
          return (v instanceof Num) ?
              self.as(Num.class, "self").eq((Num) args.get(0)):
              Bool.fal;
        }
      })
      .put(new BuiltinFunc("Num#__lt__") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArglen(args, 1);
          return self.as(Num.class, "self").lt(
              args.get(0).as(Num.class, "arg"));
        }
      })
      .hm;

  public static Num from(Double s) { return new Flt(s); }
  public static Num from(int s) { return Int.from(s); }
  public static Num from(long s) { return Int.from(s); }
  public static Num from(BigInteger s) { return Int.from(s); }
  public final HashMap<String, Val> getMeta() { return MM; }

  public abstract int asIndex();

  protected abstract Str numrepr();
  protected abstract Num hash();

  protected abstract Num add(Num n);
  protected abstract Num sub(Num n);
  protected abstract Num mul(Num n);
  protected abstract Num div(Num n);
  protected abstract Num mod(Num n);
  protected abstract Bool eq(Num n);
  protected abstract Bool lt(Num n);

  // WARNING: This subtyped arithmetic functions are called
  // with values 'reversed'. That is, 'this' is the right hand side
  // of the operation.
  protected abstract Num add(Int n);
  protected abstract Num sub(Int n);
  protected abstract Num mul(Int n);
  protected abstract Num div(Int n);
  protected abstract Num mod(Int n);
  protected abstract Bool eq(Int n);
  protected abstract Bool lt(Int n);

  protected abstract Num add(Flt n);
  protected abstract Num sub(Flt n);
  protected abstract Num mul(Flt n);
  protected abstract Num div(Flt n);
  protected abstract Num mod(Flt n);
  protected abstract Bool eq(Flt n);
  protected abstract Bool lt(Flt n);

  private static final class Int extends Num {

    public static Int from(BigInteger val) {
      return new Int(val);
    }

    public static Int from(int val) {
      return new Int(BigInteger.valueOf(val));
    }

    public static Int from(long val) {
      return new Int(BigInteger.valueOf(val));
    }

    public BigInteger val;

    private Int(BigInteger val) { this.val = val; }
    public int asIndex() { return val.intValue(); }
    protected Str numrepr() { return Str.from(val.toString()); }
    protected Num hash() { return Int.from(val.hashCode()); }

    protected Num add(Num n) { return n.add(this); }
    protected Num sub(Num n) { return n.sub(this); }
    protected Num mul(Num n) { return n.mul(this); }
    protected Num div(Num n) { return n.div(this); }
    protected Num mod(Num n) { return n.mod(this); }
    protected Bool eq(Num n) { return n.eq(this); }
    protected Bool lt(Num n) { return n.lt(this); }

    protected Num add(Int n) { return Int.from(n.val.add(val)); }
    protected Num sub(Int n) { return Int.from(n.val.subtract(val)); }
    protected Num mul(Int n) { return Int.from(n.val.multiply(val)); }
    protected Num div(Int n) { return Int.from(n.val.divide(val)); }
    protected Num mod(Int n) { return Int.from(n.val.mod(val)); }
    protected Bool eq(Int n) { return Bool.from(n.val.equals(val)); }
    protected Bool lt(Int n) { return Bool.from(n.val.compareTo(val) < 0); }

    protected Num add(Flt n) {
      return Flt.from(n.val.doubleValue() + val.doubleValue());
    }
    protected Num sub(Flt n) {
      return Flt.from(n.val.doubleValue() - val.doubleValue());
    }
    protected Num mul(Flt n) {
      return Flt.from(n.val.doubleValue() * val.doubleValue());
    }
    protected Num div(Flt n) {
      return Flt.from(n.val.doubleValue() / val.doubleValue());
    }
    protected Num mod(Flt n) {
      return Flt.from(n.val.doubleValue() % val.doubleValue());
    }
    protected Bool eq(Flt n) {
      // TODO: This has a chance of creating weird precision bugs.
      // Figure something better out here.
      return Bool.from(n.val.doubleValue() == n.val);
    }
    protected Bool lt(Flt n) {
      // TODO: This has a chance of creating weird precision bugs.
      // Figure something better out here.
      return Bool.from(n.val.doubleValue() < n.val);
    }
  }

  private static final class Flt extends Num {
    public static Flt from(Double val) { return new Flt(val); }

    public Double val;

    private Flt(Double val) { this.val = val; }

    public int asIndex() { throw new Err(toString() + " is not an int."); }
    protected Str numrepr() { return Str.from(val.toString()); }
    protected Num hash() { return Num.from(val.hashCode()); }

    protected Num add(Num n) { return n.add(this); }
    protected Num sub(Num n) { return n.sub(this); }
    protected Num mul(Num n) { return n.mul(this); }
    protected Num div(Num n) { return n.div(this); }
    protected Num mod(Num n) { return n.mod(this); }
    protected Bool eq(Num n) { return n.eq(this); }
    protected Bool lt(Num n) { return n.lt(this); }

    protected Num add(Int n) {
      return Flt.from(n.val.doubleValue() + val);
    }
    protected Num sub(Int n) {
      return Flt.from(n.val.doubleValue() - val);
    }
    protected Num mul(Int n) {
      return Flt.from(n.val.doubleValue() * val);
    }
    protected Num div(Int n) {
      return Flt.from(n.val.doubleValue() / val);
    }
    protected Num mod(Int n) {
      return Flt.from(n.val.doubleValue() % val);
    }
    protected Bool eq(Int n) {
      return Bool.from(n.val.doubleValue() == val);
    }
    protected Bool lt(Int n) {
      return Bool.from(n.val.doubleValue() < val);
    }

    protected Num add(Flt n) { return Flt.from(n.val + val); }
    protected Num sub(Flt n) { return Flt.from(n.val - val); }
    protected Num mul(Flt n) { return Flt.from(n.val * val); }
    protected Num div(Flt n) { return Flt.from(n.val / val); }
    protected Num mod(Flt n) { return Flt.from(n.val % val); }
    protected Bool eq(Flt n) { return Bool.from(n.val == val); }
    protected Bool lt(Flt n) { return Bool.from(n.val < val); }
  }
}
