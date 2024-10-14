package ast;

import token.Token;

/**
 * 函数类型节点
 * 对应文法：FuncType → 'void' | 'int' | 'char'
 */
public class FuncTypeNode {
    private String type; // 'void'，'int'，或 'char'
    private Token token;
    public FuncTypeNode(Token token) {
        this.token = token;
        this.type = token.getValue();
    }

    public String getType() {
        return type;
    }

    public Token getToken() {
        return token;
    }

    public void print() {
        if (type.equals("void")) {
            System.out.println("VOIDTK void");
        } else if (type.equals("int")) {
            System.out.println("INTTK int");
        } else if (type.equals("char")) {
            System.out.println("CHARTK char");
        }
        // 不需要输出 <FuncType>，根据您的要求
        System.out.println("<FuncType>");
    }
}
