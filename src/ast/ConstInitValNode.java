package ast;

import token.Token;

import java.util.ArrayList;
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
    /**
     * 计算常量初值的值
     *
     * @return 常量初值的值列表
     */
    public ArrayList<Integer> evaluate() {
        ArrayList<Integer> result = new ArrayList<>();

        if (constExpNode != null) {
            // 单个常量表达式
            result.add(constExpNode.evaluate());
        } else if (constExpNodeList != null) {
            // 常量表达式列表（数组）
            for (ConstExpNode expNode : constExpNodeList) {
                result.add(expNode.evaluate());
            }
        } else if (stringConst != null) {
            // 字符串常量
            for (char c : stringConst.toCharArray()) {
                result.add((int) c);
            }
            // 添加字符串结束符 '\0'
            result.add(0);
        }

        return result;
    }

    /**
     * 判断常量是否为零初始化
     *
     * @return 如果所有值均为 0，则返回 true；否则返回 false
     */
    public boolean isZero() {
        ArrayList<Integer> values = evaluate();
        for (int value : values) {
            if (value != 0) {
                return false;
            }
        }
        return true;
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
