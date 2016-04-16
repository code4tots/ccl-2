from . import err


TEMPLATE = r"""
// Cclt* -> Class name
// cclm* -> method name
// ccla* -> attribute name
// cclv* -> variable name

import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;

public class CclProgram {

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
%s
  }

  public static class CcltNil extends CcltObject {
    public CcltObject cclm_str_(int ln, CcltObject... args) {
      argscheck(args, 0);
      return new CcltString("nil");
    }
  }

  public static CcltObject nil = new CcltNil();

  public static class CcltNumber extends CcltObject {
    public final double value;
    public CcltNumber(double value) {
      this.value = value;
    }
  }

  public static class CcltString extends CcltObject {
    public final String value;

    public CcltString(String value) {
      this.value = value;
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
    cclvmain.callm_call_(0);
  }
%s
}


"""


def translate(modules):
  print(modules)
  return Translator().translate(modules)


class Translator(object):

  def __init__(self):
    self.used_methods = set()

  def translate(self, modules):
    body = self.visit_modules(modules)
    return TEMPLATE % (
        ''.join(r"""
    public CcltObject cclm%s(int ln, CcltObject... args) {
      CclException e = new CclException(
          getClass().getName() + " does not implement method");
      e.trace.add(new Trace(ln, "ccl%s"));
      throw e;
    }
""" % (method_name, method_name) for method_name in self.used_methods),
        body)

  def visit_modules(self, modules):
    return ''.join(map(self.visit_module, modules))

  def visit_module(self, module):
    _, token, _, clss, funcs, decls = module
    return ''.join((
        ''.join(map(self.visit_class, clss)),
        ''.join(map(self.visit_func, funcs)),
        ''.join(map(self.visit_decl, decls)),))

  def visit_class(self, c):
    _, token, name, base, members, methods = c
    constructor = r"""
      public Cclt%s(int ln, CcltObject... args) {
        this.constructor(ln, args);
      }
""" % name
    if not any(name is None for _, _, name, _, _, _ in methods):
      constructor = ''
    return r"""
  public static final CcltFunction cclv%s = new CcltFunction() {
    public CcltObject cclm_call_(int ln, CcltObject... args) {
      return new Cclt%s(ln, args);
    }
  }
  public static class Cclt%s extends Cclt%s {
%s
%s
%s
  }
""" % (
    name, name,
    name, base,
    ''.join(map(self.visit_member, members),
    constructor,
    ''.join(map(self.visit_method, methods))))

  def visit_method(self, func):
    _, _, name, _, _, _ = func
    self.used_methods.add(name)
    return '\npublic CcltObject cclm' + self.visit_func_core()

  def visit_func(self, func):
    return '\npublic static CcltObject cclv' + self.visit_func_core()

  def visit_func_core(self, func):
    _, token, name, argnames, varargname, body = func
    return 'ccl'


