package ast;

import token.Token;

/**
 * 基本类型节点
 * 对应文法：BType → 'int' | 'char'
 */
public class BTypeNode {
    private Token token;
    private String type; // 'int' 或 'char'

    public BTypeNode(Token token) {
        this.token = token;
        this.type = token.getValue();
    }

    public Token getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public void print() {
        if (type.equals("int")) {
            System.out.println("INTTK int");
        } else if (type.equals("char")) {
            System.out.println("CHARTK char");
        }

        // 不需要输出 <BType>
    }
}
