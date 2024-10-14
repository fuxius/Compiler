package ast;

import token.Token;

/**
 * 相等性表达式节点
 * 对应文法：EqExp → RelExp | EqExp ('==' | '!=') RelExp
 */
public class EqExpNode {
    private EqExpNode eqExpNode;
    private String op; // '==' 或 '!='
    private RelExpNode relExpNode;
    private RelExpNode singleRelExpNode;
    private Token token;

    // 单个 RelExp
    public EqExpNode(RelExpNode singleRelExpNode) {
        this.singleRelExpNode = singleRelExpNode;
    }

    public Token getToken() {
        return token;
    }

    // EqExp ('==' | '!=') RelExp
    public EqExpNode(EqExpNode eqExpNode, Token token, RelExpNode relExpNode) {
        this.eqExpNode = eqExpNode;
        this.token = token;
        this.op = token.getValue();
        this.relExpNode = relExpNode;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public String getOp() {
        return op;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public RelExpNode getSingleRelExpNode() {
        return singleRelExpNode;
    }

    public void print() {
        if (singleRelExpNode != null) {
            singleRelExpNode.print();
        } else {
            eqExpNode.print();
            if (op.equals("==")) {
                System.out.println("EQL ==");
            } else if (op.equals("!=")) {
                System.out.println("NEQ !=");
            }
            relExpNode.print();
        }
        System.out.println("<EqExp>");
    }
}
