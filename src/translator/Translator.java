
// TODO: Finish this.
public final class Translator extends AstVisitor<Void> {

  private final StringBuilder sb = new StringBuilder();

  // Statement only
  public Void visitReturn(Ast.Return node) { return null; }
  public Void visitWhile(Ast.While node) { return null; }
  public Void visitBlock(Ast.Block node) { return null; }
  public Void visitBreak(Ast.Break node) { return null; }
  public Void visitContinue(Ast.Continue node) { return null; }
  public Void visitExpressionStatement(Ast.ExpressionStatement node) {
    return null;
  }

  // Statement or Expression
  public Void visitIf(Ast.If node) { return null; }

  // Expression only
  public Void visitInt(Ast.Int node) {
    return null;
  }
  public Void visitFlt(Ast.Flt node) { return null; }
  public Void visitStr(Ast.Str node) { return null; }
  public Void visitName(Ast.Name node) { return null; }
  public Void visitAssign(Ast.Assign node) { return null; }
  public Void visitFunction(Ast.Function node) { return null; }
  public Void visitCall(Ast.Call node) { return null; }
  public Void visitGetMethod(Ast.GetMethod node) { return null; }
  public Void visitGetAttribute(Ast.GetAttribute node) { return null; }
  public Void visitSetAttribute(Ast.SetAttribute node) { return null; }
  public Void visitIs(Ast.Is node) { return null; }
  public Void visitIsNot(Ast.IsNot node) { return null; }
  public Void visitNot(Ast.Not node) { return null; }
  public Void visitAnd(Ast.And node) { return null; }
  public Void visitOr(Ast.Or node) { return null; }

  // Module
  public Void visitModule(Ast.Module node) { return null; }
}
