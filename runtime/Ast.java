package com.ccl.runtime;

import java.math.BigInteger;
import java.util.ArrayList;

public final class Ast {

  public final String filespec;
  public final int lineNumber;

  public final String type;
  public final String stringValue;
  public final BigInteger intValue;
  public final Double floatValue;
  public final ArrayList<Ast> children;

  public Ast(String filespec, int lineNumber, BigInteger intValue) {
    this(filespec, lineNumber, "Int", null, intValue, null, null);
  }

  private Ast(
      String filespec,
      int lineNumber,
      String type,
      String stringValue,
      BigInteger intValue,
      Double floatValue,
      ArrayList<Ast> children) {
    this.filespec = filespec;
    this.lineNumber = lineNumber;
    this.type = type;
    this.stringValue = stringValue;
    this.intValue = intValue;
    this.floatValue = floatValue;
    this.children = children;
  }

  private static ArrayList<Ast> toArrayList(Ast... args) {
    ArrayList<Ast> al = new ArrayList<Ast>();
    for (int i = 0; i < args.length; i++)
      al.add(args[i]);
    return al;
  }
}
