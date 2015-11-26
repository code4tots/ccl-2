// cls && javac -Xlint Easy.java TestEasy.java && java -ea TestEasy

public class TestEasy extends Easy {
  public static void main(String[] args) {
    Lexer lexer;
    Token token;

    lexer = new Lexer("hello world");

    token = lexer.next();
    assert token.type.equals("ID"): token.type;
    assert token.value.equals("hello"): token.value;

    token = lexer.next();
    assert token.type.equals("ID"): token.type;
    assert token.value.equals("world"): token.value;

    assert lexer.done();

    lexer = new Lexer("'a\\n' b 3 4.5 def [] a");

    token = lexer.next();
    assert token.type.equals("STR"): token.type;
    assert token.value.equals("a\n"): token.value;

    token = lexer.next();
    assert token.type.equals("ID"): token.type;
    assert token.value.equals("b"): token.value;

    token = lexer.next();
    assert token.type.equals("NUM"): token.type;
    assert token.value.equals(3.0): token.value;

    token = lexer.next();
    assert token.type.equals("NUM"): token.type;
    assert token.value.equals(4.5): token.value;

    token = lexer.next();
    assert token.type.equals("def"): token.type;
    assert token.value == null: token.value;

    token = lexer.next();
    assert token.type.equals("["): token.type;
    assert token.value == null: token.value;

    token = lexer.next();
    assert token.type.equals("]"): token.type;
    assert token.value == null: token.value;

    token = lexer.next();
    assert token.type.equals("ID"): token.type;
    assert token.value.equals("a"): token.value;

    assert lexer.done();

    Parser parser;
    Ast ast;

    parser = new Parser("hello[world]");
    ast = parser.parsePostfixExpression();

    assert ast instanceof CallMethodAst: ast.getClass();
    assert ast.children().length == 2: ast.children().length;
    assert ast.children()[0] instanceof NameAst:
        ast.children()[0].getClass();
    assert ast.children()[1] instanceof NameAst:
        ast.children()[1].getClass();

    parser = new Parser("List[1, 2, 3]");
    ast = parser.parsePostfixExpression();

    assert ast instanceof CallMethodAst: ast.getClass();
    assert ast.children().length == 4: ast.children().length;
    assert ast.children()[0] instanceof NameAst:
        ast.children()[0].getClass();
    assert ast.children()[1] instanceof NumberAst:
        ast.children()[1].getClass();
    assert ast.children()[2] instanceof NumberAst:
        ast.children()[2].getClass();
    assert ast.children()[3] instanceof NumberAst:
        ast.children()[3].getClass();

    Value result;

    result = eval("3");
    assert result.equals(new NumberValue(3.0)): result;

    result = eval("1 + 2");
    assert result.equals(new NumberValue(3.0)): result;

    result = eval("List[1, 2, 3]");
    assert result.equals(new ListValue(
        new NumberValue(1.0),
        new NumberValue(2.0),
        new NumberValue(3.0))): result;

    result = eval("List[1, 2, 'hi']");
    assert result.equals(new ListValue(
        new NumberValue(1.0),
        new NumberValue(2.0),
        new StringValue("hi"))): result;

    String program;

    program =
        "x = 5 " +
        "x + 6";

    result = eval(program);

    assert result.equals(new NumberValue(11.0)): result ;
  }
}
