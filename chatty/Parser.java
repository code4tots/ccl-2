package com.chatty;

import java.util.ArrayList;
import java.math.BigInteger;

public final class Parser {
  public final Lexer lexer;
  public final String name;
  public final String filespec;
  public Parser(String string, String filespec) {
    this(new Lexer(string, filespec));
  }
  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.filespec = lexer.filespec;
    this.name = filespecToName(lexer.filespec);
  }
  private Token peek() { return lexer.peek; }
  private Token next() { return lexer.next(); }
  private boolean at(String type) { return peek().type.equals(type); }
  private boolean consume(String type) {
    if (at(type)) {
      next();
      return true;
    }
    return false;
  }
  private Token expect(String type) {
    if (!at(type))
      throw new SyntaxError(
          peek(), "Expected " + type + " but found " + peek().type);
    return next();
  }
  public Ast parse() {
    Token token = peek();
    ArrayList<Ast> exprs = new ArrayList<Ast>();
    while (!at("EOF"))
      exprs.add(parseExpression());
    return new Ast.Call(token, new Ast.Name(token, "B"), exprs, null);
  }
  public Ast parseExpression() {
    return parsePostfixExpression();
  }
  public Ast parsePostfixExpression() {
    Ast node = parsePrimaryExpression();
    while (true) {
      if (at("[")) {
        Token token = expect("[");
        ArrayList<Ast> args = new ArrayList<Ast>();
        Ast vararg = null;
        while (!consume("]")) {
          if (consume("*")) {
            vararg = parseExpression();
            expect("]");
            break;
          } else {
            args.add(parseExpression());
          }
        }
        node = new Ast.Call(token, node, args, vararg);
        continue;
      }
      if (at("/")) {
        Token token = next();
        String name = (String) expect("ID").value;
        ArrayList<Ast> args = new ArrayList<Ast>();
        args.add(node);
        args.add(new Ast.Text(token, name));
        node = new Ast.Call(token, new Ast.Name(token, "__get__"), args, null);
        continue;
      }
      break;
    }
    return node;
  }

  public Ast parsePrimaryExpression() {
    if (at("STR")) {
      Token token = next();
      return new Ast.Text(token, (String) token.value);
    }
    if (at("FLT")) {
      Token token = next();
      return new Ast.Number(token, (Double) token.value);
    }
    if (at("INT")) {
      Token token = next();
      return new Ast.Number(token, ((BigInteger) token.value).doubleValue());
    }
    if (at("ID")) {
      Token token = next();
      String name = (String) token.value;
      return new Ast.Name(token, name);
    }
    throw new SyntaxError(
        peek(), "Expected expression but found " + peek().type);
  }

  public static String filespecToName(String filespec) {
    int start, end = filespec.length();
    for (start = filespec.length()-1;
          start >= 1 && filespec.charAt(start-1) != '/' &&
          filespec.charAt(start-1) != '\\'; start--);
    if (filespec.endsWith(".ccl"))
      end -= ".ccl".length();
    return filespec.substring(start, end);
  }

}
