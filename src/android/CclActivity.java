package com.ccl.android;

import android.app.Activity;
import android.util.Log;

/**
 * Created by math4tots on 3/11/16.
 */
public class CclActivity extends Activity {

  private Runtime runtime = null;

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (runtime == null) {
      runtime = new Runtime(this);
      runtime.importModule("main");
    }
    runtime.onCreate();
  }

  @Override
  protected void onStart() {
    super.onStart();
    runtime.onStart();
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    runtime.onRestart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    runtime.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    runtime.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    runtime.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    runtime.onDestroy();
  }

}
