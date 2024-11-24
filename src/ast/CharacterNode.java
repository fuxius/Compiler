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
        // 从字符常量中获取第一个字符，并返回其 ASCII 值,并特殊处理转义字符
        if (charConst.charAt(1) == '\\') {
            switch (charConst.charAt(2)) {
                case '0':
                    return 0;
                case 'n':
                    return 10;
                case 'r':
                    return 13;
                case 't':
                    return 9;
                case '\\':
                    return 92;
                case '\'':
                    return 39;
                case '\"':
                    return 34;
                default:
                    return charConst.charAt(2);
            }
        }
        return charConst.charAt(1);
    }
}
