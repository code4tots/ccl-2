package com.ccl.desktop;

import com.ccl.core.*;
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

public class Core extends com.ccl.core.Runtime {

  private final String pathToModules;
  public Core(String pathToModules) {
    this.pathToModules = pathToModules;
  }

  protected void populateGlobalScope(Scope scope) {
    super.populateGlobalScope(scope);
    scope
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
  }

  protected Ast.Module loadModule(String uri) {
    return readModule(makePath(pathToModules, uri + ".ccl"));
  }

  public void runModule(String path) {
    new Scope(global).eval(readModule(path));
  }

  public static void main(String[] args) {
    try {
      Core desktop = new Core(makePath(args[0], "mods"));
      desktop.importModule("corelib");
      desktop.importModule("corelib_desktop");
      desktop.runModule(args[1]);
   } catch (final Err e) {
      System.out.println(e.toString() + e.getTraceString());
      e.printStackTrace();
      System.exit(1);
    } catch (final Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
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
