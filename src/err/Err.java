package com.ccl.core;

import java.util.ArrayList;

public final class Err extends RuntimeException {
  private static final long serialVersionUID = 42L;

  private ArrayList<Traceable> trace = new ArrayList<Traceable>();

  public Err(String message) {
    super(message);
  }

  public Err(String message, Traceable traceable) {
    super(message);
    trace.add(traceable);
  }
}
