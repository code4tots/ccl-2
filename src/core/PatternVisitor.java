package com.ccl.core;

public abstract class PatternVisitor<T> {
  public final void visit(Pattern pattern, T t) { pattern.accept(this, t); }
  public abstract void visitList(Pattern.List pattern, T t);
  public abstract void visitName(Pattern.Name pattern, T t);
}
