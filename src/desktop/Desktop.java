import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.io.File;

public class Desktop {

  static {
    Val.MODULE_REGISTRY.put("gui", Gui.MODULE);
  }

  public static String PATH_TO_MODULES = null;

  public static final Scope DESKTOP_GLOBAL = new Scope()
      .put(new BuiltinFunc("write") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArgRange(args, 1, 2);

          // Special case when printing to STDOUT.
          if (args.size() == 1 || args.get(1) == Nil.val)
            System.out.print(args.get(0));

          else {
            // Here, we know that we are going to open a file.

            // TODO: Test this better.
            String content = args.get(0).toString();
            String path = args.get(1).as(Str.class, "argument 1").val;

            PrintWriter writer;
            try { writer = new PrintWriter(path, "UTF-8"); }
            catch (IOException e) { throw new Err(e); }

            writer.print(content);
            writer.close();
          }
          return args.get(0);
        }
      })
      .put(new BuiltinFunc("import") {
        public Val calli(Val self, ArrayList<Val> args) {
          String name = args.get(0).as(Str.class, "argument").val;

          if (Val.MODULE_REGISTRY.get(name) == null) {
            Scope scope = new Scope(DESKTOP_GLOBAL);
            scope.eval(readModule(makePath(PATH_TO_MODULES, name + ".ccl")));
            Val.MODULE_REGISTRY.put(
                name,
                new Blob(Val.MMModule, scope.table));
          }

          return Err.notNull(Val.MODULE_REGISTRY.get(name));
        }
      })
      .put(new BuiltinFunc("read") {
        public Val calli(Val self, ArrayList<Val> args) {
          Err.expectArgRange(args, 0, 1);
          String path =
              args.size() == 0 ?
              "<stdin>":
              args.get(0).as(Str.class, "arg").val;
          Reader reader;
          try {
            reader =
                args.size() == 0 ?
                new InputStreamReader(System.in):
                new FileReader(path);
          } catch (IOException e) {
            throw new Err(e);
          }
          return Str.from(readFile(reader, path));
        }
      })
      ;

  public static void main(String[] args) {
    try {
      PATH_TO_MODULES = makePath(args[0], "mods");
      final String PATH_TO_CORELIB = makePath(PATH_TO_MODULES, "corelib.ccl");
      final String PATH_TO_CORELIB_DESKTOP =
          makePath(PATH_TO_MODULES, "corelib_desktop.ccl");

      new Scope(DESKTOP_GLOBAL).eval(readModule(PATH_TO_CORELIB));
      new Scope(DESKTOP_GLOBAL).eval(readModule(PATH_TO_CORELIB_DESKTOP));
      new Scope(DESKTOP_GLOBAL).eval(readModule(args[1]));
    } catch (final Err e) {
      System.out.println(e.toString() + e.getTraceString());
      e.printStackTrace();
      System.exit(1);
    } catch (final Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
    // System.out.println(ProfilingEvaluator.getResultSummary());
  }

  public static String makePath(String start, String... args) {
    File f = new File(start);
    for (int i = 0; i < args.length; i++)
      f = new File(f, args[i]);
    return f.getPath();
  }

  public static String readFile(String path) {
    Reader r;
    try {
      r = new FileReader(path);
    } catch (IOException e) {
      throw new Err(e);
    }
    return readFile(r, path);
  }

  public static String readFile(Reader unBufferedreader, String path) {
    try {
      BufferedReader reader = new BufferedReader(unBufferedreader);
      String line = null;
      StringBuilder sb = new StringBuilder();
      String separator = System.getProperty("line.separator");

      while((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append(separator);
      }

      return sb.toString();
    } catch (IOException e) {
      throw new Err(e);
    }
  }

  public static Ast.Module readModule(String path) {
    return new Parser(readFile(path), path).parse();
  }
}
