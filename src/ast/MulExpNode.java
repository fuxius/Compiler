package ast;

import token.Token;

/**
 * 乘除模表达式节点
 * 对应文法：MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
 */
public class MulExpNode {
    private MulExpNode mulExpNode;
    private String op; // '*', '/', '%'
    private UnaryExpNode unaryExpNode;
    private UnaryExpNode singleUnaryExpNode;
    private Token token;

    // 单个 UnaryExp
    public MulExpNode(UnaryExpNode singleUnaryExpNode) {
        this.singleUnaryExpNode = singleUnaryExpNode;
    }

    public Token getToken() {
        return token;
    }

    // MulExp ('*' | '/' | '%') UnaryExp
    public MulExpNode(MulExpNode mulExpNode, Token token, UnaryExpNode unaryExpNode) {
        this.mulExpNode = mulExpNode;
        this.token= token;
        this.op = token.getValue();
        this.unaryExpNode = unaryExpNode;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public String getOp() {
        return op;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public UnaryExpNode getSingleUnaryExpNode() {
        return singleUnaryExpNode;
    }

    public void print() {
        if (singleUnaryExpNode != null) {
            singleUnaryExpNode.print();
        } else {
            mulExpNode.print();
            if (op.equals("*")) {
                System.out.println("MULT *");
            } else if (op.equals("/")) {
                System.out.println("DIV /");
            } else if (op.equals("%")) {
                System.out.println("MOD %");
            }
            unaryExpNode.print();
        }
        System.out.println("<MulExp>");
    }
}
