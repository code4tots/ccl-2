package com.ccl.core;

import java.util.ArrayList;

public final class Err extends RuntimeException {
  private static final long serialVersionUID = 42L;

  private ArrayList<Traceable> trace = new ArrayList<Traceable>();

  public Err(String message) {
    super(message);
  }

  public Err(Throwable throwable) {
    super(throwable);
  }

  public Err(String message, Traceable traceable) {
    super(message);
    trace.add(traceable);
  }

  public Err(Throwable throwable, Traceable traceable) {
    super(throwable);
    trace.add(traceable);
  }

  public void add(Traceable traceable) {
    trace.add(traceable);
  }

  public String getTraceString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < trace.size(); i++) {
      sb.append(trace.get(i).getTraceMessage());
    }
    return sb.toString();
  }

  public String getMessageWithTrace() {
    return toString() + getTraceString().replace("\n", "\n  ");
  }
}
