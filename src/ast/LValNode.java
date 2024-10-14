package ast;

import token.Token;

import java.util.List;

/**
 * 左值表达式节点
 * 对应文法：LVal → Ident ['[' Exp ']']
 */
public class LValNode {
    private String ident;
    private ExpNode expNode; // 可选的数组下标

    private Token token;

    public Token getToken() {
        return token;
    }

    public LValNode(Token token, ExpNode expNode) {
        this.token = token;
        this.ident = token.getValue();
        this.expNode = expNode;
    }

    public String getIdent() {
        return ident;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);
        if (expNode != null) {
            System.out.println("LBRACK [");
            expNode.print();
            System.out.println("RBRACK ]");
        }
        System.out.println("<LVal>");
    }
}
