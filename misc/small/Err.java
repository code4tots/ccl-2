package com.ccl.core;

public final class Err extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final String message;
  public Err(String message) {
    this.message = message;
  }
}
