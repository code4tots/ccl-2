package com.ccl;

// SyntaxError is thrown if an error is encountered in either
// the lex or parse process.
public final class SyntaxError extends RuntimeException {
  public static final long serialVersionUID = 42L;
  public final Token token;
  public final String message;
  public SyntaxError(Token token, String message) {
    super(message + "\n" + token.getLocationString());
    this.token = token;
    this.message = message;
  }
}
