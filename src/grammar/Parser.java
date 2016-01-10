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
  public Ast.Module parse() {

    Token token = peek();
    ArrayList<Ast> exprs = new ArrayList<Ast>();
    while (!at("EOF"))
      exprs.add(parseStatement());

    return new Ast.Module(token, name, new Ast.Block(token, exprs));
  }
  public Ast parseStatement() {

    if (at("{")) {
      Token token = next();
      ArrayList<Ast> exprs = new ArrayList<Ast>();
      while (!at("}"))
        exprs.add(parseStatement());
      expect("}");
      return new Ast.Block(token, exprs);
    }

    if (at("return")) {
      Token token = next();
      Ast value = parseExpression();
      return new Ast.Return(token, value);
    }

    if (at("while")) {
      Token token = next();
      Ast cond = parseExpression();
      Ast body = parseStatement();
      return new Ast.While(token, cond, body);
    }

    if (at("if")) {
      Token token = next();
      Ast cond = parseExpression();
      consume("then");
      Ast body = parseStatement();
      Ast other = new Ast.Block(token, new ArrayList<Ast>());
      if (consume("else"))
        other = parseStatement();
      return new Ast.If(token, cond, body, other);
    }

    if (at("break"))
      return new Ast.Break(next());

    if (at("continue"))
      return new Ast.Continue(next());

    return parseExpression();
  }
  public Ast parseExpression() {
    return parseOrExpression();
  }
  public Ast parseOrExpression() {
    Ast node = parseAndExpression();
    while (true) {
      if (at("or")) {
        Token token = next();
        Ast right = parseAndExpression();
        node = new Ast.Or(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseAndExpression() {
    Ast node = parseCompareExpression();
    while (true) {
      if (at("and")) {
        Token token = next();
        Ast right = parseCompareExpression();
        node = new Ast.And(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseCompareExpression() {
    Ast node = parseAdditiveExpression();
    while (true) {
      if (at("==")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__eq__", right);
        continue;
      }
      if (at("!=")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__ne__", right);
        continue;
      }
      if (at("<")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__lt__", right);
        continue;
      }
      if (at("<=")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__le__", right);
        continue;
      }
      if (at(">")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__gt__", right);
        continue;
      }
      if (at(">=")) {
        Token token = next();
        Ast right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__ge__", right);
        continue;
      }
      if (at("is")) {
        Token token = next();
        if (consume("not")) {
          Ast right = parseAdditiveExpression();
          node = new Ast.IsNot(token, node, right);
        } else {
          Ast right = parseAdditiveExpression();
          node = new Ast.Is(token, node, right);
        }
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseAdditiveExpression() {
    Ast node = parseMultiplicativeExpression();
    while (true) {
      if (at("+")) {
        Token token = next();
        Ast right = parseMultiplicativeExpression();
        node = new Ast.Call(token, node, "__add__", right);
        continue;
      }
      if (at("-")) {
        Token token = next();
        Ast right = parseMultiplicativeExpression();
        node = new Ast.Call(token, node, "__sub__", right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parseMultiplicativeExpression() {
    Ast node = parsePrefixExpression();
    while (true) {
      if (at("*")) {
        Token token = next();
        Ast right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__mul__", right);
        continue;
      }
      if (at("/")) {
        Token token = next();
        Ast right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__div__", right);
        continue;
      }
      if (at("%")) {
        Token token = next();
        Ast right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__mod__", right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parsePrefixExpression() {
    // Negative/positive numeric signs for constants must be
    // handled here because otherwise, we wouldn't be able to
    // distinguish between 'x-1' meaning 'x', '-', '1' or
    // 'x', '-1'.
    if (at("+")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      return new Ast.Call(token, node, "__pos__");
    }
    if (at("-")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      return new Ast.Call(token, node, "__neg__");
    }
    if (at("not")) {
      Token token = next();
      Ast node = parsePrefixExpression();
      return new Ast.Not(token, node);
    }
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
            consume(",");
          }
        }

        if (at("=")) {
          token = next();
          if (vararg != null || args.size() != 1)
            throw new SyntaxError(
                token, "For setitem syntax, must have exactly one argument");
          node = new Ast.Call(
              token, node, "__setitem__", args.get(0), parseExpression());
        } else {
          node = new Ast.Call(token, node, "__call__", args, vararg);
        }
        continue;
      }

      if (at("@")) {
        Token token = next();
        String name = (String) expect("ID").value;
        if (at("=")) {
          token = next();
          Ast value = parseExpression();
          node = new Ast.SetAttribute(token, node, name, value);
        } else {
          node = new Ast.GetAttribute(token, node, name);
        }
        continue;
      }

      if (at(".")) {
        Token token = next();
        String name = (String) expect("ID").value;
        if (consume("[")) {
          ArrayList<Ast> args = new ArrayList<Ast>();
          Ast vararg = null;
          while (!consume("]")) {
            if (consume("*")) {
              vararg = parseExpression();
              expect("]");
              break;
            } else {
              args.add(parseExpression());
              consume(",");
            }
          }
          node = new Ast.Call(token, node, name, args, vararg);
        }
        else
          node = new Ast.GetMethod(token, node, name);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast parsePrimaryExpression() {

    if (at("STR")) {
      Token token = next();
      return new Ast.Str(token, (String) token.value);
    }

    if (at("FLT")) {
      Token token = next();
      return new Ast.Flt(token, (Double) token.value);
    }

    if (at("INT")) {
      Token token = next();
      return new Ast.Int(token, (BigInteger) token.value);
    }

    if (at("ID")) {
      Token token = next();
      String name = (String) token.value;

      if (at("=")) {
        token = next();
        Ast value = parseExpression();
        return new Ast.Assign(token, name, value);
      } else {
        return new Ast.Name(token, name);
      }
    }

    if (consume("(")) {
      Ast expr = parseExpression();
      expect(")");
      return expr;
    }

    if (at("\\") || at("\\\\")) {
      boolean newScope = at("\\");
      Token token = next();
      ArrayList<String> args = new ArrayList<String>();
      String vararg = null;
      while (at("ID")) {
        args.add((String) expect("ID").value);
        consume(",");
      }
      ArrayList<String> optargs = new ArrayList<String>();
      while (consume("/")) {
        optargs.add((String) expect("ID").value);
        consume(",");
      }
      if (consume("*"))
        vararg = (String) expect("ID").value;
      consume(".");
      Ast body = parseStatement();
      return new Ast.Function(token, args, optargs, vararg, body, newScope);
    }

    if (at("if")) {
      Token token = next();
      Ast cond = parseExpression();
      consume("then");
      Ast body = parseExpression();
      Ast other = new Ast.Name(token, "nil");
      if (consume("else"))
        other = parseExpression();
      return new Ast.If(token, cond, body, other);
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
