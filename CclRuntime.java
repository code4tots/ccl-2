
// Cclt* -> Class name
// cclm* -> method name
// ccla* -> attribute name
// cclv* -> variable name

// TODO: Maybe fold this into the translator so that
// I don't have to rely on reflection for every method call.
// Instead, CcltObject could be filled with stubs of every possible
// method name.

import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;

public class CclRuntime {

  public static class Trace {
    public final int lineno;
    public final String message;
    public Trace(int lineno, String message) {
      this.lineno = lineno;
      this.message = message;
    }
  }

  public static class CclException extends RuntimeException {
    ArrayList<Trace> trace = new ArrayList<Trace>();
    CclException(String message) { super(message); }
  }

  public static void argscheck(CcltObject[] args, int len) {
    if (args.length != len) {
      throw new CclException(
          "Expected " + len + " arguments but found " + args.length);
    }
  }

  public static class CcltObject {
    public CcltObject call(String name, int lineno, CcltObject... args) {
      try {
        return (CcltObject) getClass()
            .getMethod(name, int.class, CcltObject[].class)
            .invoke(this, lineno, args);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw (RuntimeException) e.getCause();
      }
    }

    public String toString() {
      return call("cclm_str_", 0).toString();
    }
  }

  public static class CcltNil extends CcltObject {
    public CcltObject cclm_str_(int ln, CcltObject... args) {
      argscheck(args, 0);
      return new CcltString("nil");
    }
  }

  public static CcltObject nil = new CcltNil();

  public static class CcltString extends CcltObject {
    public final String value;

    public CcltString(String s) {
      value = s;
    }

    public CcltObject cclm_str_(int ln, CcltObject... args) {
      argscheck(args, 0);
      return this;
    }

    public String toString() {
      return value;
    }
  }

  public abstract static class CcltFunction extends CcltObject {
    public abstract CcltObject cclm_call_(int lineno, CcltObject... args);
  }

  public static final CcltFunction cclvprint = new CcltFunction() {
    public CcltObject cclm_call_(int lineno, CcltObject... args) {
      try {
        argscheck(args, 1);
        System.out.println(args[0].call("cclm_str_", 0));
        return nil;
      } catch (final CclException e) {
        e.trace.add(new Trace(lineno, "print"));
        throw e;
      }
    }
  };

  public static void main(String[] args) {
    cclvprint.cclm_call_(0, nil);
  }
}
