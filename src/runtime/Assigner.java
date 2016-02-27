package com.ccl.core;

import java.util.ArrayList;
import java.util.Iterator;

public final class Assigner extends PatternVisitor<Value> {
  private final Scope scope;
  public Assigner(Scope scope) {
    this.scope = scope;
  }

  public void visitList(Pattern.List pattern, Value val) {
    // TODO: Allow 'val' to be any iteratorable.
    ArrayList<Value> args = val.as(List.class).getValue();
    if (pattern.vararg == null) {
      if (pattern.optargs.size() == 0)
        ErrUtils.expectArglen(args, pattern.args.size());
      else
        ErrUtils.expectArgRange(
            args, pattern.args.size(),
            pattern.args.size() + pattern.optargs.size());
    } else {
      ErrUtils.expectMinArglen(args, args.size());
    }

    Iterator<Value> vit = args.iterator();
    Iterator<Pattern> sit = pattern.args.iterator();

    while (sit.hasNext())
      visit(sit.next(), vit.next());

    sit = pattern.optargs.iterator();

    while (sit.hasNext() && vit.hasNext())
      visit(sit.next(), vit.next());

    while (sit.hasNext())
      visit(sit.next(), Nil.value);

    if (pattern.vararg != null) {
      ArrayList<Value> va = new ArrayList<Value>();
      while (vit.hasNext())
        va.add(vit.next());
      scope.put(pattern.vararg, List.from(va));
    }
  }

  public void visitName(Pattern.Name pattern, Value value) {
    scope.put(pattern.name, value);
  }
}
