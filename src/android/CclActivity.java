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
  }
}
