public abstract class AstVisitor<T> {
  public final T visit(Ast node) { return node.accept(this); }

  // Statement only
  public abstract T visitReturn(Ast.Return node);
  public abstract T visitWhile(Ast.While node);
  public abstract T visitBlock(Ast.Block node);
  public abstract T visitBreak(Ast.Break node);
  public abstract T visitContinue(Ast.Continue node);

  // Statement or Expression
  public abstract T visitIf(Ast.If node);

  // Expression only
  public abstract T visitInt(Ast.Int node);
  public abstract T visitFlt(Ast.Flt node);
  public abstract T visitStr(Ast.Str node);
  public abstract T visitName(Ast.Name node);
  public abstract T visitAssign(Ast.Assign node);
  public abstract T visitFunction(Ast.Function node);
  public abstract T visitCall(Ast.Call node);
  public abstract T visitGetMethod(Ast.GetMethod node);
  public abstract T visitGetAttribute(Ast.GetAttribute node);
  public abstract T visitSetAttribute(Ast.SetAttribute node);
  public abstract T visitIs(Ast.Is node);
  public abstract T visitIsNot(Ast.IsNot node);
  public abstract T visitNot(Ast.Not node);
  public abstract T visitAnd(Ast.And node);
  public abstract T visitOr(Ast.Or node);

  // Module
  public abstract T visitModule(Ast.Module node);
}
