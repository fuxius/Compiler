package ast;

import token.Token;

/**
 * 单目运算符节点
 * 对应文法：UnaryOp → '+' | '−' | '!'
 */
public class UnaryOpNode {
    private String op; // '+', '-', '!'
    private Token token;

    public UnaryOpNode(Token token) {
        this.token = token;
        this.op = token.getValue();
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public String getOp() {
        return op;
    }

    public void print() {
        if (op.equals("+")) {
            System.out.println("PLUS +");
        } else if (op.equals("-")) {
            System.out.println("MINU -");
        } else if (op.equals("!")) {
            System.out.println("NOT !");
        }
        System.out.println("<UnaryOp>");
    }
}
