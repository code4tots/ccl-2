package com.ccl.android;

import android.util.Log;

import com.ccl.core.Blob;
import com.ccl.core.BuiltinFunction;
import com.ccl.core.Err;
import com.ccl.core.ErrUtils;
import com.ccl.core.List;
import com.ccl.core.Scope;
import com.ccl.core.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Runtime extends com.ccl.core.Runtime {

  public final CclActivity activity;

  public Runtime(CclActivity activity) {
    this.activity = activity;
    moduleRegistry.put(
        "rt/android",
        new Blob(Blob.MODULE_META));
    importModule("rt/android_prelude");
  }

  @Override
  public void populateGlobalScope(Scope scope) {
    super.populateGlobalScope(scope);
    scope
        .put("print", new BuiltinFunction("print") {
          @Override
          public Value calli(Value owner, List args) {
            ErrUtils.expectArglen(args, 1);
            Log.d("ccl_print", args.get(0).toString());
            return args.get(0);
          }
        });
  }

  @Override
  public String readModule(String uri) {
    try {
      return readFile(new InputStreamReader(
          activity.getAssets().open("cclmods/" + uri + ".ccl")));
    } catch (IOException e) {
      throw new Err(e);
    }
  }

  private static String readFile(Reader unBufferedreader) {
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
