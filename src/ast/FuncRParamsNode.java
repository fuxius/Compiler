package ast;

import java.util.List;

/**
 * 函数实参表节点
 * 对应文法：FuncRParams → Exp { ',' Exp }
 */
public class FuncRParamsNode {
    private List<ExpNode> expNodes;

    public FuncRParamsNode(List<ExpNode> expNodes) {
        this.expNodes = expNodes;
    }

    public void print() {
        for (int i = 0; i < expNodes.size(); i++) {
            expNodes.get(i).print();
            if (i < expNodes.size() - 1) {
                System.out.println("COMMA ,");
            }
        }
        System.out.println("<FuncRParams>");

    }
}
