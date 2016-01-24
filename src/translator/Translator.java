
// TODO: Finish this.
public final class Translator extends AstVisitor<String> {

  // Statement only
  public String visitReturn(Ast.Return node) {
    return "\nreturn " + visit(node.val) + ";";
  }
  public String visitWhile(Ast.While node) {
    return
      "\nwhile (" + visit(node.cond) + ".truthy())" +
      visit(node.body).replaceAll("\n", "\n  ");
  }
  public String visitBlock(Ast.Block node) { return null; }
  public String visitBreak(Ast.Break node) { return null; }
  public String visitContinue(Ast.Continue node) { return null; }
  public String visitExpressionStatement(Ast.ExpressionStatement node) {
    return "\n" + visit(node.expr) + ";";
  }

  // Statement or Expression
  public String visitIf(Ast.If node) {
    String translation =
      "\nif (" + visit(node.cond) + ")" +
        visit(node.body).replaceAll("\n", "\n  ");

    if (node.other != null)
      translation +=
        "\nelse" +
        visit(node.other).replaceAll("\n", "\n  ");

    return translation;
  }

  // Expression only
  public String visitInt(Ast.Int node) {
    // TODO: Make these ints constants constructed on startup.
    return "Num.from(\"" + node.val.toString() + "\")";
  }
  public String visitFlt(Ast.Flt node) {
    // TODO: Make these floats constants constructed on startup.
    return "Num.from(" + node.val.toString() + ")";
  }
  public String visitStr(Ast.Str node) { return null; }
  public String visitName(Ast.Name node) {
    // TODO: Die if name is not defined.
    return "scope.getOrNull(" + node.name + ")";
  }
  public String visitAssign(Ast.Assign node) { return null; }
  public String visitFunction(Ast.Function node) { return null; }
  public String visitCall(Ast.Call node) { return null; }
  public String visitGetMethod(Ast.GetMethod node) { return null; }
  public String visitGetAttribute(Ast.GetAttribute node) { return null; }
  public String visitSetAttribute(Ast.SetAttribute node) { return null; }
  public String visitIs(Ast.Is node) { return null; }
  public String visitIsNot(Ast.IsNot node) { return null; }
  public String visitNot(Ast.Not node) { return null; }
  public String visitAnd(Ast.And node) { return null; }
  public String visitOr(Ast.Or node) { return null; }

  // Module
  public String visitModule(Ast.Module node) { return null; }


  // Just sanity check things work as expected.
  public static void main(String[] args) {
    System.out.println(new Translator().visit(new Parser("1", "<test>").parseStatement()));
    System.out.println(new Translator().visit(new Parser("hi", "<test>").parseStatement()));
    System.out.println(new Translator().visit(new Parser("while (1) 2.0", "<test>").parseStatement()));
  }
}
