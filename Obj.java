import java.util.ArrayList;
import java.util.HashMap;

public class Obj {

  // Convenience converters
  public static Obj X(Integer i) { return new Num(i); }
  public static Obj X(Double d) { return new Num(d); }
  public static Obj X(String s) { return new Str(s); }
  public static Obj A(Obj... args) { return new List(args); }
  public static Obj D(Obj... args) { return new Dict(args); }

  // globals
  public static int OBJECT_COUNT = 0;
  public static final Obj TYPE_TYPE = new TypeType();
  public static final Obj OBJ_TYPE = new Type();
  public static final Obj NIL_TYPE = new Type(OBJ_TYPE) {
    public Obj call(Obj... args) {
      requireExactArgs(0, args);
      return NIL;
    }
  };
  public static final Obj BOOL_TYPE = new Type(OBJ_TYPE) {
    public Obj call(Obj... args) {
      requireExactArgs(1, args);
      return args[0].toBoolean() ? TRUE : FALSE;
    }
  };
  public static final Obj NUM_TYPE = new Type(OBJ_TYPE);
  public static final Obj STR_TYPE = new Type(OBJ_TYPE) {
    public Obj call(Obj... args) {
      requireExactArgs(1, args);
      return X(args[0].toString());
    }
  };
  public static final Obj LIST_TYPE = new Type(OBJ_TYPE);
  public static final Obj DICT_TYPE = new Type(OBJ_TYPE);
  public static final Obj CALLABLE_TYPE = new Type(OBJ_TYPE);
  public static final Obj FUNCTION_TYPE = new Type(CALLABLE_TYPE);
  public static final Obj METHOD_TYPE = new Type(CALLABLE_TYPE);

  public static final Obj NIL = new Nil();
  public static final Obj TRUE = new Bool(true);
  public static final Obj FALSE = new Bool(false);

  // methods on these standard values.
  static {
    NIL_TYPE.setattr(X("__str__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        return X("nil");
      }
    });

    BOOL_TYPE.setattr(X("__str__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        return X(args[0].toBoolean() ? "true" : "false");
      }
    });

    NUM_TYPE.setattr(X("__str__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        requireInstanceOf(args[0], NUM_TYPE);
        Double value = args[0].toDouble();

        if (value.equals(Math.floor(value)) && !Double.isInfinite(value))
          return X(Integer.toString(value.intValue()));
        else
          return X(value.toString());
      }
    });

    NUM_TYPE.setattr(X("__add__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(2, args);
        requireInstanceOf(args[0], NUM_TYPE);
        requireInstanceOf(args[1], NUM_TYPE);
        return X(args[0].toDouble() + args[1].toDouble());
      }
    });

    // Note, Str.toString will not actually call this.
    STR_TYPE.setattr(X("__str__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        requireInstanceOf(args[0], STR_TYPE);
        return args[0];
      }
    });

    LIST_TYPE.setattr(X("__str__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        return X("<List>"); // TODO
      } 
    });

    LIST_TYPE.setattr(X("size"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        requireInstanceOf(args[0], LIST_TYPE);
        return X(args[0].toArrayList().size());
      } 
    });

    LIST_TYPE.setattr(X("__setitem__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(3, args);
        requireInstanceOf(args[0], LIST_TYPE);
        requireInstanceOf(args[1], NUM_TYPE);
        args[0].toArrayList().set(args[1].toInteger(), args[2]);
        return NIL;
      } 
    });

    LIST_TYPE.setattr(X("__getitem__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(2, args);
        requireInstanceOf(args[0], LIST_TYPE);
        requireInstanceOf(args[1], NUM_TYPE);
        return args[0].toArrayList().get(args[1].toInteger());
      } 
    });

    DICT_TYPE.setattr(X("__str__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(1, args);
        return X("<Dict>"); // TODO
      } 
    });

    DICT_TYPE.setattr(X("__setitem__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(3, args);
        requireInstanceOf(args[0], DICT_TYPE);
        args[0].toHashMap().put(args[1], args[2]);
        return NIL;
      } 
    });

    DICT_TYPE.setattr(X("__getitem__"), new Function() {
      public Obj call(Obj... args) {
        requireExactArgs(2, args);
        requireInstanceOf(args[0], DICT_TYPE);
        return args[0].toHashMap().get(args[1]);
      } 
    });
  }

  // Members shared by all Obj.
  protected final int uid;
  protected final Obj type;
  protected final HashMap<String, Obj> attrs = new HashMap<String, Obj>();

  // TODO: Maybe, maybe implement meta-classes in the future.
  // But the way things are implemented here may make that difficult.

  /*
  Java subclasses of Obj should be significantly special enough to warrant
  subclassing. Most OOP is done through the 'type' attribute instead of
  Java's subclassing mechanism. Java's subclassing is used when either

    1) Requires specialized Java types (e.g. any members that are not Obj).
    2) Requires a specialized implementation of a method (e.g. getattr/setattr)
  */
  protected Obj() {
    // TODO: Branch out the Java inheritance tree such that
    // all non-TypeType subclasses into a separate root class
    // so that this is not an issue.
    if (OBJECT_COUNT != 0)
      throw err("TARFU");

    if (!(this instanceof TypeType))
      throw err("Only TypeType can use this constructor but found type: " + getClass().getName());

    uid = OBJECT_COUNT++;
    type = this;
  }

  public Obj(Obj type) {
    if (OBJECT_COUNT == 0)
      throw err("TARFU");

    if (!(type instanceof Type) && !(type instanceof TypeType))
      throw err("Type must be a Java subclass of Type or TypeType but found: " + getClass().getName());

    uid = OBJECT_COUNT++;
    this.type = type;
  }

  public RuntimeException err(String message) {
    // TODO: Make error messages location aware.
    return new RuntimeException(this.getClass().getName() + ": " + message);
  }

  public void requireInstanceOf(Obj instance, Obj cls) {
    // TODO
  }

  public void requireExactArgs(int len, Obj[] args) {
    if (len != args.length)
      throw err("Expected " + new Integer(len).toString() + " but found " + new Integer(args.length).toString() + " args");
  }

  public void requireAtLeastArgs(int len, Obj[] args) {
    if (len >= args.length)
      throw err("Expected at least " + new Integer(len).toString() + " but found " + new Integer(args.length).toString() + " args");
  }

  public final boolean hasattr(String name) {
    return xgetattr(name) != null;
  }

  public final boolean hasattr(Obj name) {
    if (!(name instanceof Str))
      throw err("hasattr with type: " + name.getClass().getName());
    return hasattr(name.toString());
  }

  protected Obj xgetattr(String name) {
    Obj attr = type.xgetattr(name);
    if (attr != null && ((attr instanceof Callable) || attr.hasattr("__call__")))
      attr = new Method(this, attr);
    if (attr == null)
      attr = attrs.get(name);
    return attr;
  }

  public final Obj getattr(String name) {
    Obj attr = xgetattr(name);
    if (attr == null)
      throw err("No such attribute: " + name);
    return attr;
  }

  public final Obj getattr(Obj name) {
    if (!(name instanceof Str))
      throw err("getattr with type: " + name.getClass().getName());
    return getattr(name.toString());
  }

  public final void setattr(String name, Obj value) {
    if (value == null)
      throw new NullPointerException("WTF. attr = " + name);
    attrs.put(name, value);
  }

  public final void setattr(Obj name, Obj value) {
    if (!(name instanceof Str))
      throw err("setattr with type: " + name.getClass().getName());
    setattr(name.toString(), value);
  }

  public Obj call(Obj... args) {
    return getattr("__call__").call(args);
  }

  // Java bridge methods
  public boolean equals(Object other) {
    if (!(other instanceof Obj))
      return false;
    return eq((Obj) other);
  }

  public boolean eq(Obj other) {
    if (hasattr("__eq__"))
      return getattr("__eq__").call(other).toBoolean();
    else
      return uid == other.uid;
  }

  public int hashCode() { return 0; }

  public Boolean toBoolean() {
    if (hasattr("__bool__"))
      return getattr("__bool__").call().toBoolean();
    return true;
  }

  public Double toDouble() {
    if (hasattr("__num__"))
      return getattr("__num__").call().toDouble();
    throw err("Converting to Double not supported");
  }

  public Integer toInteger() {
    throw err("Converting to Integer not supported");
  }

  public String toString() {
    Obj str = getattr("__str__").call();
    if (!(str instanceof Str))
      throw err("__str__ must return a str!");
    return str.toString();
  }

  public ArrayList<Obj> toArrayList() {
    throw err("Converting to ArrayList not supported");
  }

  public HashMap<Obj, Obj> toHashMap() {
    throw err("Converting to HashMap not supported");
  }

  // TODO: Maybe make TypeType a subclass of Type and deal with the repurcussions.
  public static class TypeType extends Obj {
    public TypeType() { super(); }

    protected Obj xgetattr(String name) {
      return attrs.get(name);
    }

    public Obj call(Obj... args) {
      requireExactArgs(1, args);
      return args[0].type;
    }
  }

  // TODO: Right now MRO is DFS. Change this to C3 algorithm (i.e. what Python does nowadays).
  public static class Type extends Obj {
    private final ArrayList<Obj> bases = new ArrayList<Obj>();

    public Type(Obj... bases) {
      super(TYPE_TYPE);
      for (int i = 0; i < bases.length; i++)
        this.bases.add(bases[i]);
    }

    protected Obj xgetattr(String name) {
      Obj attr = super.xgetattr(name);
      for (int i = 0; i < bases.size(); i++)
        if (bases.get(i).hasattr(name))
          return bases.get(i).getattr(name);
      return attr;
    }

    public Obj call(Obj... args) {
      Obj self = new Obj(this);
      Obj init = xgetattr("__init__");
      if (init == null) {
        requireExactArgs(0, args);
      }
      else {
        new Method(self, init).call(args);
      }
      return self;
    }
  }

  public static class Nil extends Obj {
    public Nil() { super(NIL_TYPE); }

    public boolean eq(Obj other) {
      return (other instanceof Nil);
    }

    public Boolean toBoolean() { return false; }
  }

  public static class Bool extends Obj {
    private final Boolean value;
    public Bool(Boolean value) { super(BOOL_TYPE); this.value = value; }

    public boolean eq(Obj other) {
      return (other instanceof Bool) && value.equals(other.toBoolean());
    }

    public Boolean toBoolean() { return value; }
  }

  public static class Num extends Obj {
    private final Double value;
    public Num(Double value) { super(NUM_TYPE); this.value = value; }
    public Num(Integer value) { super(NUM_TYPE); this.value = value.doubleValue(); }

    public boolean eq(Obj other) {
      return (other instanceof Num) && value.equals(other.toDouble());
    }

    public Boolean toBoolean() { return value != 0; }
    public Double toDouble() { return value; }
    public Integer toInteger() { return value.intValue(); }
  }

  public static class Str extends Obj {
    private final String value;
    public Str(String value) { super(STR_TYPE); this.value = value; }

    public boolean eq(Obj other) {
      return (other instanceof Str) && value.equals(other.toString());
    }

    public Boolean toBoolean() { return value.length() > 0; }
    public String toString() { return value; }
  }

  public static class List extends Obj {
    private final ArrayList<Obj> value = new ArrayList<Obj>();
    public List(Obj... args) {
      super(LIST_TYPE);

      for (int i = 0; i < args.length; i++)
        value.add(args[i]);
    }

    public boolean eq(Obj other) {
      return (other instanceof List) && value.equals(other.toArrayList());
    }

    public Boolean toBoolean() { return value.size() > 0; }
    public ArrayList<Obj> toArrayList() { return value; }
  }

  public static class Dict extends Obj {
    private final HashMap<Obj, Obj> value = new HashMap<Obj, Obj>();
    public Dict(Obj... args) {
      super(DICT_TYPE);

      if (args.length % 2 != 0)
        throw err("Dict constructor must be given an even number of arguments");

      for (int i = 0; i < args.length; i += 2)
        value.put(args[i], args[i+1]);
    }

    public boolean eq(Obj other) {
      return (other instanceof Dict) && value.equals(other.toHashMap());
    }

    public Boolean toBoolean() { return value.size() > 0; }
    public HashMap<Obj, Obj> toHashMap() { return value; }
  }

  abstract public static class Callable extends Obj {
    public Callable(Obj type) { super(type); }
  }

  /*
  You might wonder whether the 'Method' class really should be a Java subclass.
  Strictly speaking, I don't think so. Maybe in the future I'll undo this.
  */
  public static class Method extends Callable {
    private Obj owner, callable;
    public Method(Obj owner, Obj callable) { super(METHOD_TYPE); this.owner = owner; this.callable = callable; }

    public Obj call(Obj... args) {
      Obj[] newargs = new Obj[1 + args.length];
      newargs[0] = owner;
      for (int i = 0; i < args.length; i++)
        newargs[i+1] = args[i];
      return callable.call(newargs);
    }
  }

  abstract public static class Function extends Callable {
    public Function() { super(FUNCTION_TYPE); }

    abstract public Obj call(Obj... args);
  }

  // assert is a keyword.
  public static void require(Boolean value, String message) {
    if (!value)
      throw new RuntimeException("assertion error: " + message);
  }

  public static void test() {
    require(NIL.toString().equals("nil"), "NIL.toString() should be 'nil'");
    require(TRUE.toString().equals("true"), "TRUE.toString() should be 'true'");
    require(FALSE.toString().equals("false"), "FALSE.toString() should be 'false'");
    require(X(0).toString().equals("0"), "Zero toString should be '0' but got: " + X(0).toString());
    require(X(0.1).toString().equals("0.1"), "(0.1)toString should be '0.1' but got: " + X(0.1).toString());
  }

  public static void main(String[] args) {
    test();
  }
}
