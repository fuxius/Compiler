package ast;

import token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量初值节点
 * 对应文法：
 * InitVal → Exp
 *            | '{' [ Exp { ',' Exp } ] '}'
 *            | StringConst
 */
public class InitValNode {
    private ExpNode expNode;
    private List<ExpNode> expNodeList;
    private String stringConst;
    private Token token;

    // 单个表达式
    public InitValNode(ExpNode expNode) {
        this.expNode = expNode;
    }

    // 表达式列表（数组）
    public InitValNode(List<ExpNode> expNodeList) {
        this.expNodeList = expNodeList;
    }

    public Token getToken() {
        return token;
    }

    // 字符串常量
    public InitValNode(Token token) {
        this.token = token;
        this.stringConst = token.getValue();
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public List<ExpNode> getExpNodeList() {
        return expNodeList;
    }

    public String getStringConst() {
        return stringConst;
    }

    public boolean isZero(){
        return expNode == null && expNodeList.size() == 0;
    }
    // 评估初始值
    public ArrayList<Integer> evaluate() {
        ArrayList<Integer> values = new ArrayList<>();

        // 如果是单个表达式
        if (expNode != null) {
            values.add(expNode.evaluate()); // 调用 ExpNode 的 evaluate 方法
        }
        // 如果是表达式列表
        else if (expNodeList != null) {
            for (ExpNode exp : expNodeList) {
                values.add(exp.evaluate()); // 对每个表达式求值
            }
        }
        // 如果是字符串常量
        else if (stringConst != null) {
            for (char c : stringConst.toCharArray()) {
                values.add((int) c); // 将字符串拆分为字符 ASCII 值
            }
        }

        return values;
    }
    public void print() {
        if (expNode != null) {
            expNode.print();
        } else if (expNodeList != null) {
            System.out.println("LBRACE {");
            for (int i = 0; i < expNodeList.size(); i++) {
                expNodeList.get(i).print();
                if (i < expNodeList.size() - 1) {
                    System.out.println("COMMA ,");
                }
            }
            System.out.println("RBRACE }");
        } else if (stringConst != null) {
            System.out.println("STRCON " + stringConst);
        }
        System.out.println("<InitVal>");
    }
}
