package com.ccl.desktop;

import com.ccl.core.*;
import com.ccl.core.Number;

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

public class Runtime extends com.ccl.core.Runtime {

  public final String pathToModules;

  public Runtime(String pathToModules) {
    this.pathToModules = pathToModules;
    importModule("rt/desktop_prelude");
  }

  @Override
  public void populateGlobalScope(Scope scope) {
    super.populateGlobalScope(scope);
    scope
        .put("print", new BuiltinFunction("print") {
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 1);
            System.out.println(args.get(0));
            return args.get(0);
          }
        })
        .put("time", new BuiltinFunction("time") {
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 0);
            return Number.from(
                ((double)System.currentTimeMillis())/1000);
          }
        });
  }

  @Override
  public String readModule(String uri) {
    return readFile(makePath(pathToModules, uri + ".ccl"));
  }

  public static void main(String[] args) {
    try {
      Runtime runtime = new Runtime(makePath(args[0], "mods"));
      runtime.runMainModule(readFile(args[1]));
    } catch (final Err e) {
      System.out.println(e.getMessageWithTrace());
      // TODO: Add a flag for displaying native Java errors.
      e.printStackTrace();
      System.exit(1);
    } catch (final Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  // File finding and reading helpers.
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
    return readFile(r);
  }

  public static String readFile(Reader unBufferedreader) {
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
}
