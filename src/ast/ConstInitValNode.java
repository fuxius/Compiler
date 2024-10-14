package ast;

import token.Token;

import java.util.List;

/**
 * 常量初值节点
 * 对应文法：
 * ConstInitVal → ConstExp
 *               | '{' [ ConstExp { ',' ConstExp } ] '}'
 *               | StringConst
 */
public class ConstInitValNode {
    private ConstExpNode constExpNode;
    private List<ConstExpNode> constExpNodeList;
    private String stringConst;
    private Token token;

    // 单个常量表达式
    public ConstInitValNode(ConstExpNode constExpNode) {
        this.constExpNode = constExpNode;
    }

    public Token getToken() {
        return token;
    }

    // 常量表达式列表（数组）
    public ConstInitValNode(List<ConstExpNode> constExpNodeList) {
        this.constExpNodeList = constExpNodeList;
    }

    // 字符串常量
    public ConstInitValNode(Token token) {
        this.token = token;
        this.stringConst = token.getValue();
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public List<ConstExpNode> getConstExpNodeList() {
        return constExpNodeList;
    }

    public String getStringConst() {
        return stringConst;
    }

    public void print() {
        if (constExpNode != null) {
            constExpNode.print();
        } else if (constExpNodeList != null) {
            System.out.println("LBRACE {");
            for (int i = 0; i < constExpNodeList.size(); i++) {
                constExpNodeList.get(i).print();
                if (i < constExpNodeList.size() - 1) {
                    System.out.println("COMMA ,");
                }
            }
            System.out.println("RBRACE }");
        } else if (stringConst != null) {
            System.out.println("STRCON " + stringConst);
        }

        System.out.println("<ConstInitVal>");
    }
}
