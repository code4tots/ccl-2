
// TODO: Finish this.
public final class Translator extends AstVisitor<String> {

  // Statement only
  public String visitReturn(Ast.Return node) {
    return "\nreturn " + visit(node.val) + ";";
  }
  public String visitWhile(Ast.While node) {

    return null;
  }
  public String visitBlock(Ast.Block node) { return null; }
  public String visitBreak(Ast.Break node) { return null; }
  public String visitContinue(Ast.Continue node) { return null; }
  public String visitExpressionStatement(Ast.ExpressionStatement node) {
    return null;
  }

  // Statement or Expression
  public String visitIf(Ast.If node) { return null; }

  // Expression only
  public String visitInt(Ast.Int node) {
    return null;
  }
  public String visitFlt(Ast.Flt node) { return null; }
  public String visitStr(Ast.Str node) { return null; }
  public String visitName(Ast.Name node) { return null; }
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
}
