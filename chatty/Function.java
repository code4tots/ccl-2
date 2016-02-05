package com.chatty;

import java.util.ArrayList;

public abstract class Function extends Callable {
  public final Value call(Scope scope, ArrayList<Ast> rawArgs) {
    List args = new List();
    for (int i = 0; i < rawArgs.size(); i++)
      args.add(rawArgs.get(i).eval(scope));
    return call(args);
  }
  public abstract Value call(List args);
}
