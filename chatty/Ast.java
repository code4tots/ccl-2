package com.chatty;

import java.util.ArrayList;

public abstract class Ast {
  public final Token token;
  public Ast(Token token) { this.token = token; }

  public static final class Call extends Ast {
    public final Ast f;
    public final ArrayList<Ast> args;
    public final Ast vararg; // nullable
    public Call(Token token, Ast f, ArrayList<Ast> args, Ast vararg) {
      super(token);
      this.f = f;
      this.args = args;
      this.vararg = vararg;
    }
  }

  public static final class Number extends Ast {
    public final Double value;
    public Number(Token token, Double value) {
      super(token);
      this.value = value;
    }
  }

  public static final class Text extends Ast {
    public final String value;
    public Text(Token token, String value) {
      super(token);
      this.value = value;
    }
  }

  public static final class Name extends Ast {
    public final String value;
    public Name(Token token, String value) {
      super(token);
      this.value = value;
    }
  }
}
