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
    /**
     * Evaluate 方法返回字符常量的 ASCII 值。
     * @return ASCII值，字符常量在ASCII表中对应的整数值。
     */
    public int evaluate() {
        // 从字符常量中获取第一个字符，并返回其 ASCII 值
        return charConst.charAt(1);
    }
}
