import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;

public class Desktop {

  public static final String PATH_TO_CORELIB =
      makePath(
          System.getProperty("user.home"),
          "git", "ccl", "src", "corelib.ccl");

  static {
    Scope.GLOBAL.put(new BuiltinFunc("print") {
      public Val calli(Val self, ArrayList<Val> args) {
        System.out.println(args.get(0));
        return args.get(0);
      }
    });
  }

  public static void main(String[] args) {
    try {
      new Scope(Scope.GLOBAL).eval(readModule(PATH_TO_CORELIB));
      new Scope(Scope.GLOBAL).eval(readModule(args[0]));
    } catch (final Err e) {
      System.out.println(e.toString() + e.getTraceString());
      throw e;
    }
  }

  public static String makePath(String start, String... args) {
    File f = new File(start);
    for (int i = 0; i < args.length; i++)
      f = new File(f, args[i]);
    return f.getPath();
  }

  public static String readFile(String path) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String line = null;
      StringBuilder sb = new StringBuilder();
      String separator = System.getProperty("line.separator");

      while((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append(separator);
      }

      return sb.toString();
    } catch (IOException e) {
      throw new RuntimeException(
          "Exception while reading " + path + ": " + e.toString());
    }
  }

  public static Ast.Module readModule(String path) {
    return new Parser(readFile(path), path).parse();
  }

}
