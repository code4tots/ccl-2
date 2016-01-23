package com.ccl.runtime;

import java.util.StringTokenizer;

public final class Loader {

  // This static method is the only public interface to this class.
  public static Ast load(String text) {
    return new Loader(new StringTokenizer(text)).load();
  }

  // Implementation detail.

  private final String filespec;
  private final StringTokenizer tokenizer;
  private String peek;

  private Loader(StringTokenizer tokenizer) {
    this.tokenizer = tokenizer;
    this.filespec = tokenizer.nextToken();
    this.peek = tokenizer.nextToken();
  }

  private String next() {
    if (peek == null)
      throw new RuntimeException("EOF");
    String next = peek;
    if (tokenizer.hasMoreTokens())
      peek = tokenizer.nextToken();
    else
      peek = null;
    return next;
  }

  private Ast load() {
    return null; // TODO
  }
}
