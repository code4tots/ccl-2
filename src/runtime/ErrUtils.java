package com.ccl.core;

import java.util.ArrayList;

public final class ErrUtils {
  private ErrUtils() {}

  public static void expectArgRange(ArrayList<Value> args, int min, int max) {
    if (args.size() < min || args.size() > max)
      throw new Err(
          "Expected " + min + " to " + max + " arguments but got " +
          args.size() + ".");
  }

  public static void expectArglen(List args, int len) {
    expectArglen(args.getValue(), len);
  }

  public static void expectArglen(ArrayList<Value> args, int len) {
    if (args.size() != len)
      throw new Err(
          "Expected " + len + " arguments but got " +
          args.size() + ".");
  }

  public static void expectMinArglen(ArrayList<Value> args, int len) {
    if (args.size() < len)
      throw new Err(
          "Expected at least " + len + " arguments but got only " +
          args.size() + ".");
  }

  public static void expectArglens(ArrayList<Value> args, int... lens) {
    for (int i = 0; i < lens.length; i++)
      if (args.size() == lens[i])
        return;
    StringBuilder sb = new StringBuilder("Expected ");
    for (int i = 0; i < lens.length-1; i++)
      sb.append(lens[i] + " or ");
    sb.append(lens[lens.length-1] + " arguments but found " + args.size());
    throw new Err(sb.toString());
  }
}