package ast;

import token.Token;

/**
 * 字符节点
 * 对应文法：Character → CharConst
 */
public class CharacterNode {
    private String charConst;
    private Token token;

    public Token getToken() {
        return token;
    }

    public CharacterNode(Token token) {
        this.token = token;
        this.charConst = token.getValue();
    }

    public String getCharConst() {
        return charConst;
    }

    public void print() {
        System.out.println("CHARCON " + charConst);
        // 不需要输出 <Character>，根据您的要求
        System.out.println("<Character>");
    }
}
