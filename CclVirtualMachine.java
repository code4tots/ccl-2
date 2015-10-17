import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public class CclVirtualMachine {

  public static RuntimeException err(String message) { return new RuntimeException(message); }

  abstract public static class Obj implements Comparable<Obj> {
    public static int OBJECT_COUNT = 0;
    public final int id;
    Obj() { id = OBJECT_COUNT++; }
    abstract public String getTypeId();

    // For my language, instead of Comparable, I want objects to be Diffable.
    // Often times, when comparing objects, we want to know what is different about
    // two objects, not just that they are different.
    public Obj diff(Obj x) {
      if (this == x)
        return new Nil();
      if (getTypeId() != x.getTypeId())
        return new List(
            new Str("Different types"),                               // 0: Type of diff
            new Num((double) getTypeId().compareTo(x.getTypeId())),   // 1: result of compareTo
            new List(new Str(getTypeId()), new Str(x.getTypeId())));  // 2: Evidence
      return diffWithObjectOfThisType(x);
    }
    abstract public Obj diffWithObjectOfThisType(Obj x);
    public int compareTo(Obj x) { return ((Num)((List)diff(x)).val.get(1)).val.intValue(); }
    public boolean equals(Obj x) { return diff(x) instanceof Nil; }

    // toString is important for debugging purposes
    public String toString() { return repr(); }
    abstract public String repr();
  }
  public static class Nil extends Obj {
    public static final String TYPE_ID = "Nil";
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) { return new Nil(); }

    public String repr() { return "nil"; }
  }
  public static class Num extends Obj {
    public static final String TYPE_ID = "Num";
    public final Double val;
    public Num(Double v) { val = v; }
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) {
      int cmp = val.compareTo(((Num)x).val);
      return cmp == 0 ? new Nil() : new List(
          new Str("Different numbers"),
          new Num((double) cmp),
          new List(this, x));
    }

    public String repr() { return val.toString(); }
  }
  public static class Str extends Obj {
    public static final String TYPE_ID = "Str";
    public final String val;
    public Str(String v) { val = v; }
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) {
      int cmp = val.compareTo(((Str)x).val);
      return cmp == 0 ? new Nil() : new List(
          new Str("Different strings"),
          new Num((double) cmp),
          new List(this, x));
    }

    public String toString() { return val; }
    public String repr() { return "\"" + val.replace("\"", "\\\"") + "\""; }
  }
  public static class List extends Obj {
    public static final String TYPE_ID = "List";
    public final ArrayList<Obj> val = new ArrayList<Obj>();
    public List(Obj... args) {
      for (int i = 0; i < args.length; i++)
        val.add(args[i]);
    }
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) {
      ArrayList<Obj> v = ((List)x).val;
      int len = val.size() < v.size() ? val.size() : v.size();
      for (int i = 0; i < len; i++) {
        Obj dx = val.get(i).diff(v.get(i));
        if (!(dx instanceof Nil))
          return new List(new Str("List item different at index: " + Integer.toString(i)), ((List)dx).val.get(1), dx);
      }
      if (val.size() != v.size())
        return new List(
            new Str("List sizes don't match"),
            new Num((double) (val.size() - v.size())),
            new List(new Num((double) val.size()), new Num((double) v.size())));
      return new Nil();
    }

    public String repr() {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      sb.append("[");
      for (int i = 0; i < val.size(); i++) {
        if (!first)
          sb.append(", ");
        sb.append(val.get(i).repr());
        first = false;
      }
      sb.append("]");
      return sb.toString();
    }
  }
  public static class Dict extends Obj {
    public static final String TYPE_ID = "Dict";
    public final TreeMap<Obj, Obj> val = new TreeMap<Obj, Obj>();
    public Dict(Obj... args) {
      for (int i = 0; i < args.length; i += 2)
        val.put(args[i], args[i+1]);
    }
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) {
      TreeMap <Obj, Obj> v = ((Dict)x).val;
      Iterator<Obj> a = val.keySet().iterator(), b = v.keySet().iterator();
      while (a.hasNext() && b.hasNext()) {
        Obj z = a.next(), y = b.next(), dx = z.diff(y);
        if (!(dx instanceof Nil))
          return new List(new Str("Different keys"), ((List)dx).val.get(1), dx);
        Object key = z;
        z = val.get(z);
        y = v.get(y);
        dx = z.diff(y);
        if (!(dx instanceof Nil))
          return new List(new Str("Different values for key: " + key.toString()), ((List)dx).val.get(1), dx);
      }
      if (a.hasNext() || b.hasNext()) {
        Obj key = a.hasNext() ? a.next() : b.next();
        return new List(new Str("Key " + key.toString() + "in one dict but not the other"), new Num((double) (val.size() - v.size())), key);
      }
      return new Nil();
    }
    public String repr() {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      Iterator<Obj> it = val.keySet().iterator();
      sb.append("{");
      while (it.hasNext()) {
        Obj key = it.next();
        if (!first)
          sb.append(", ");
        first = true;
        sb.append(key.repr());
        sb.append(": ");
        sb.append(val.get(key).repr());
      }
      sb.append("}");
      return sb.toString();
    }
  }
  public static class Macro extends Obj {
    public static final String TYPE_ID = "Macro";
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) {
      return id == x.id ? new Nil() : new List(
          new Str("Different macros"),
          new Num((double) (id - x.id)),
          new List(new Num((double) id), new Num((double) x.id)));
    }
    public String repr() { return "<macro " + Integer.toString(hashCode()) + ">"; }
  }
  public static class Func extends Obj {
    public static final String TYPE_ID = "Func";
    public String getTypeId() { return TYPE_ID; }
    public Obj diffWithObjectOfThisType(Obj x) {
      return id == x.id ? new Nil() : new List(
          new Str("Different functions"),
          new Num((double) (id - x.id)),
          new List(new Num((double) id), new Num((double) x.id)));
    }
    public String repr() { return "<func " + Integer.toString(hashCode()) + ">"; }
  }
  public static void main(String[] args) {
    System.out.println(new Dict().diff(new Dict()));
    System.out.println(new Num(4.0).diff(new Num(4.0)));
    System.out.println(new Num(4.0).diff(new Num(5.0)));
    System.out.println(new List(new Num(4.0)).diff(new Num(5.0)));
    System.out.println(new List(new Num(4.0)).diff(new List(new Num(5.0))));
  }
}
