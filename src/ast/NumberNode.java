package ast;

import token.Token;

/**
 * 数值节点
 * 对应文法：Number → IntConst
 */
public class NumberNode {
    private String intConst;
    private Token token;

    public Token getToken() {
        return token;
    }

    public NumberNode(Token token) {
        this.token = token;
        this.intConst = token.getValue();
    }

    public String getIntConst() {
        return intConst;
    }

    public void print() {
        System.out.println("INTCON " + intConst);
        System.out.println("<Number>");
        // 不需要输出 <Number>，根据您的要求
    }
}
