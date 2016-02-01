package com.ccl;

import java.util.ArrayList;

public final class Err extends RuntimeException {
  public final ArrayList<Traceable> trace = new ArrayList<Traceable>();
  public Err(String message) { super(message); }
  public Err(Throwable cause) { super(cause); }
  public String getTraceString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < trace.size(); i++)
      sb.append(trace.get(i).getTraceMessage());
    return sb.toString();
  }
}
