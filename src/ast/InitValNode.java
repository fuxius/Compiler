package ast;

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

    // 单个表达式
    public InitValNode(ExpNode expNode) {
        this.expNode = expNode;
    }

    // 表达式列表（数组）
    public InitValNode(List<ExpNode> expNodeList) {
        this.expNodeList = expNodeList;
    }

    // 字符串常量
    public InitValNode(String stringConst) {
        this.stringConst = stringConst;
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
