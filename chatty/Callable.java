package com.chatty;

import java.util.ArrayList;

public abstract class Callable extends Value {
  public abstract Value call(Scope scope, ArrayList<Ast> args);
}
