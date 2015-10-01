import java.util.ArrayList;
import java.util.HashMap;

public class Obj {

  public final Obj type;
  public final HashMap<String, Obj> attrs = new HashMap<String, Obj>();

  protected Obj() {
    type = this;
  }

  public Obj(Obj type, Obj... args) {
    this.type = type;
    Obj init = type.attrs.get("__init__");
    if (init != null)
      init.bind(this).call(args);
  }

  public Obj getattr(String name) {
    Obj attr = attrs.get(name);
    if (attr == null) {
      attr = type.attrs.get(name);
      if (attr == null) {
        Obj mroobj = type.attrs.get("__mro__");
        if (mroobj != null) {
          ArrayList<Obj> mro = mroobj.toArrayList();
          for (int i = 0; i < mro.size() && attr == null; i++)
            attr = mro.get(i).attrs.get(name);
        }
      }
      attr = attr.bind(this);
    }
    return attr;
  }

  public Obj xgetattr(String name) {
    Obj attr = getattr(name);
    if (attr == null)
      throw err("Attribute " + name + " not in " + toString());
    return attr;
  }

  public Obj bind(Obj owner) {
    return this;
  }

  public Obj call(Obj... args) {
    return m("__call__", args);
  }

  public final Obj m(String name, Obj... args) {
    return xgetattr(name).call(args);
  }

  // convert to java types.

  public Boolean toBoolean() {
    Obj x = m("__bool__", args);
    if (x instanceof Bool)
      throw err("__bool__ should've returned bool but got " + x.toString());
    return x.toBoolean();
  }

  public Double toDouble() {
    Obj x = m("__num__", args);
    if (x instanceof Num)
      throw err("__num__ should've returned num but got " + x.toString());
    return x.toDouble();
  }

  public String toString() {
    Obj x = m("__str__", args);
    if (x instanceof Str)
      throw err("__str__ should've returned str but got " + x.toString());
    return x.toString();
  }

  public ArrayList<Obj> toArrayList() {
    Obj x = m("__list__", args);
    if (x instanceof List)
      throw err("__list__ should've returned list but got " + x.toString());
    return x.toArrayList();
  }

  public HashMap<Obj, Obj> toHashMap() {
    Obj x = m("__dict__", args);
    if (x instanceof Dict)
      throw err("__dict__ should've returned dict but got " + x.toString());
    return x.toHashMap();
  }

  // static

  public static final Obj TYPE_TYPE = new Obj();

  public static RuntimeError err(String message) {
    return new RuntimeError(message);
  }

  public static RuntimeError err(Obj message) {
    return err(message.toString());
  }

}
