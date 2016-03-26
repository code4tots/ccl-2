package com.ccl.android;

import android.util.Log;

import com.ccl.core.Blob;
import com.ccl.core.BuiltinFunction;
import com.ccl.core.Err;
import com.ccl.core.ErrUtils;
import com.ccl.core.List;
import com.ccl.core.Nil;
import com.ccl.core.Scope;
import com.ccl.core.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Runtime extends com.ccl.core.Runtime {

  public final CclActivity activity;

  private Value onCreateCallback = null;
  private Value onStartCallback= null;
  private Value onRestartCallback = null;
  private Value onResumeCallback = null;
  private Value onPauseCallback = null;
  private Value onStopCallback= null;
  private Value onDestroyCallback = null;

  public Runtime(CclActivity activity) {
    this.activity = activity;
    moduleRegistry.put(
        "rt/android",
        new Blob(Blob.MODULE_META)
          .setattr("onCreate", new BuiltinFunction("rt/android@onCreate") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onCreateCallback = args.get(0);
              return Nil.value;
            }
          })
          .setattr("onStart", new BuiltinFunction("rt/android@onStart") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onStartCallback = args.get(0);
              return Nil.value;
            }
          })
          .setattr("onRestart", new BuiltinFunction("rt/android@onRestart") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onRestartCallback = args.get(0);
              return Nil.value;
            }
          })
          .setattr("onResume", new BuiltinFunction("rt/android@onResume") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onResumeCallback = args.get(0);
              return Nil.value;
            }
          })
          .setattr("onPause", new BuiltinFunction("rt/android@onPause") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onPauseCallback = args.get(0);
              return Nil.value;
            }
          })
          .setattr("onStop", new BuiltinFunction("rt/android@onStop") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onStopCallback = args.get(0);
              return Nil.value;
            }
          })
          .setattr("onDestroy", new BuiltinFunction("rt/android@onDestroy") {
            @Override
            public Value calli(Value owner, List args) {
              ErrUtils.expectArglen(args, 1);
              onDestroyCallback = args.get(0);
              return Nil.value;
            }
          }));
    importModule("rt/android_prelude");
  }

  public void onCreate() {
    if (onCreateCallback != null) {
      onCreateCallback.callf(List.from());
    }
  }

  public void onStart() {
    if (onStartCallback != null) {
      onStartCallback.callf(List.from());
    }
  }

  public void onRestart() {
    if (onRestartCallback != null) {
      onRestartCallback.callf(List.from());
    }
  }

  public void onResume() {
    if (onResumeCallback != null) {
      onResumeCallback.callf(List.from());
    }
  }

  public void onPause() {
    if (onPauseCallback != null) {
      onPauseCallback.callf(List.from());
    }
  }

  public void onStop() {
    if (onStopCallback != null) {
      onStopCallback.callf(List.from());
    }
  }

  public void onDestroy() {
    if (onDestroyCallback != null) {
      onDestroyCallback.callf(List.from());
    }
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
      return readFile(new InputStreamReader(activity.getAssets().open(
          "cclmods/" + uri + ".ccl")));
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
