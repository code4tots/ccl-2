package com.ccl.javascript;

import com.ccl.core.*;

/*
Runtime variables that need to be available in Javascript:
  scope
  CclCallMethod(owner, methodName, args)
  CclIsTruthy(value)
  CclAnd(left, right) // right is a callback
  CclOr(left, right) // right is a callback
*/
public final class Translator extends AstVisitor<Void> {

  private StringBuilder sb = new StringBuilder();
  private int lexicalScopeDepth = 0;

  @Override
  public String toString() {
    return sb.toString();
  }

  // Statement only
  @Override
  public Void visitReturn(Ast.Return node) {
    sb.append("return ");
    visit(node.val);
    sb.append(";");
    return null;
  }

  @Override
  public Void visitIf(Ast.If node) {
    sb.append("if (CclIsTruthy(");
    visit(node.cond);
    sb.append(")) {");
    visit(node.body);
    sb.append("} else {");
    visit(node.other);
    sb.append("}");
    return null;
  }

  @Override
  public Void visitWhile(Ast.While node) {
    sb.append("while (CclIsTruthy(");
    visit(node.cond);
    sb.append(")) {");
    visit(node.body);
    sb.append("}");
    return null;
  }

  @Override
  public Void visitBlock(Ast.Block node) {
    sb.append("{");
    for (Ast ast: node.body) {
      visit(ast);
    }
    sb.append("}");
    return null;
  }

  @Override
  public Void visitBreak(Ast.Break node) {
    sb.append("break;");
    return null;
  }

  @Override
  public Void visitContinue(Ast.Continue node) {
    sb.append("continue;");
    return null;
  }

  @Override
  public Void visitExpressionStatement(Ast.ExpressionStatement node) {
    visit(node.expr);
    sb.append(";");
    return null;
  }

  // Expression only
  @Override
  public Void visitInt(Ast.Int node) {
    sb.append(node.val.toString());
    return null;
  }

  @Override
  public Void visitFlt(Ast.Flt node) {
    sb.append(node.val.toString());
    return null;
  }

  @Override
  public Void visitStr(Ast.Str node) {
    // TODO: Better string escape.
    sb.append("\"");
    sb.append(node.val
        .replace("\"", "\\\"")
        .replace("\n", "\\\n"));
    sb.append("\"");
    return null;
  }

  @Override
  public Void visitName(Ast.Name node) {
    sb.append("scope.cclid_");
    sb.append(node.name);
    return null;
  }

  @Override
  public Void visitAssign(Ast.Assign node) {
    new PatternTranslator().visit(node.pattern, sb);
    sb.append("=");
    visit(node.val);
    return null;
  }

  @Override
  public Void visitFunction(Ast.Function node) {
    sb.append("function(){");

    // Create a new scope if prompted to do so.
    if (node.newScope) {
      sb.append("var scope=Object.create(scope");
      sb.append(lexicalScopeDepth);
      sb.append(");");

      lexicalScopeDepth++;

      sb.append("var scope");
      sb.append(lexicalScopeDepth);
      sb.append("=scope;");
    }

    // Assign arguments.
    new PatternTranslator().visit(node.args, sb);
    sb.append("=arguments");

    visit(node.body);

    if (node.newScope) {
      lexicalScopeDepth--;
    }

    sb.append("}");
    return null;
  }

  @Override
  public Void visitCall(Ast.Call node) {
    sb.append("CclCallMethod(");
    visit(node.owner);
    sb.append(", \"" + node.name + "\", [");
    for (Ast arg: node.args) {
      visit(arg);
      sb.append(",");
    }
    sb.append("]");
    if (node.vararg != null) {
      sb.append(".concat(");
      visit(node.vararg);
      sb.append(")");
    }
    return null;
  }

  @Override
  public Void visitGetMethod(Ast.GetMethod node) {
    throw new Err("Not implemented");
  }

  @Override
  public Void visitGetAttribute(Ast.GetAttribute node) {
    sb.append("(");
    visit(node.owner);
    sb.append(".attrs.cclid_");
    sb.append(node.name);
    sb.append(")");
    return null;
  }

  @Override
  public Void visitSetAttribute(Ast.SetAttribute node) {
    sb.append("(");
    visit(node.owner);
    sb.append(".attrs.cclid_");
    sb.append(node.name);
    sb.append("=");
    visit(node.val);
    sb.append(")");
    return null;
  }

  @Override
  public Void visitIs(Ast.Is node) {
    sb.append("(");
    visit(node.left);
    sb.append("===");
    visit(node.right);
    sb.append(")");
    return null;
  }

  @Override
  public Void visitIsNot(Ast.IsNot node) {
    sb.append("(");
    visit(node.left);
    sb.append("!==");
    visit(node.right);
    sb.append(")");
    return null;
  }

  @Override
  public Void visitNot(Ast.Not node) {
    sb.append("(!");
    visit(node.target);
    sb.append(")");
    return null;
  }

  @Override
  public Void visitAnd(Ast.And node) {
    sb.append("CclAnd(");
    visit(node.left);
    sb.append(",function(){return ");
    visit(node.right);
    sb.append("})");
    return null;
  }

  @Override
  public Void visitOr(Ast.Or node) {
    sb.append("CclOr(");
    visit(node.left);
    sb.append(",function(){return ");
    visit(node.right);
    sb.append("})");
    return null;
  }

  @Override
  public Void visitTernary(Ast.Ternary node) {
    sb.append("(CclIsTruthy(");
    visit(node.cond);
    sb.append(")?");
    visit(node.body);
    sb.append(":");
    visit(node.other);
    sb.append(")");
    return null;
  }

  // Module
  @Override
  public Void visitModule(Ast.Module node) {
    sb.append("function(){'use strict';");
    visit(node.body);
    sb.append("}");
    return null;
  }
}
