package ast;

import token.Token;

/**
 * 关系表达式节点
 * 对应文法：RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
 */
public class RelExpNode {
    private RelExpNode relExpNode;
    private String op; // '<', '>', '<=', '>='
    private Token token;
    private AddExpNode addExpNode;
    private AddExpNode singleAddExpNode;

    // 单个 AddExp
    public RelExpNode(AddExpNode singleAddExpNode) {
        this.singleAddExpNode = singleAddExpNode;
    }

    // RelExp ('<' | '>' | '<=' | '>=') AddExp
    public RelExpNode(RelExpNode relExpNode, Token token, AddExpNode addExpNode) {
        this.relExpNode = relExpNode;
        this.token = token;
        this.op = token.getValue();
        this.addExpNode = addExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public String getOp() {
        return op;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public AddExpNode getSingleAddExpNode() {
        return singleAddExpNode;
    }

    public void print() {
        if (singleAddExpNode != null) {
            singleAddExpNode.print();
        } else {
            relExpNode.print();
            switch (op) {
                case "<":
                    System.out.println("LSS <");
                    break;
                case ">":
                    System.out.println("GRE >");
                    break;
                case "<=":
                    System.out.println("LEQ <=");
                    break;
                case ">=":
                    System.out.println("GEQ >=");
                    break;
            }
            addExpNode.print();
        }
        System.out.println("<RelExp>");
    }
}
