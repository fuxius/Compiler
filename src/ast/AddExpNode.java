package ast;

import token.Token;

/**
 * 加减表达式节点
 * 对应文法：AddExp → MulExp | AddExp ('+' | '−') MulExp
 */
public class AddExpNode {
    private AddExpNode addExpNode;
    private String op; // '+' 或 '-'
    private MulExpNode mulExpNode;
    private MulExpNode singleMulExpNode;
    private Token token;

    // 单个 MulExp
    public AddExpNode(MulExpNode singleMulExpNode) {
        this.singleMulExpNode = singleMulExpNode;
    }

    // AddExp ('+' | '−') MulExp
    public AddExpNode(AddExpNode addExpNode, Token token, MulExpNode mulExpNode) {
        this.addExpNode = addExpNode;
        this.token = token;
        this.op = token.getValue();
        this.mulExpNode = mulExpNode;
    }

    public Token getToken() {
        return token;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public String getOp() {
        return op;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public MulExpNode getSingleMulExpNode() {
        return singleMulExpNode;
    }

    public void print() {
        if (singleMulExpNode != null) {
            singleMulExpNode.print();
        } else {
            addExpNode.print();
            if (op.equals("+")) {
                System.out.println("PLUS +");
            } else if (op.equals("-")) {
                System.out.println("MINU -");
            }
            mulExpNode.print();
        }
        System.out.println("<AddExp>");
    }
}
