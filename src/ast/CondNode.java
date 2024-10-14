package ast;

/**
 * 条件表达式节点
 * 对应文法：Cond → LOrExp
 */
public class CondNode {
    private LOrExpNode lOrExpNode;

    public CondNode(LOrExpNode lOrExpNode) {
        this.lOrExpNode = lOrExpNode;
    }

    public LOrExpNode getlOrExpNode() {
        return lOrExpNode;
    }

    public void print() {
        lOrExpNode.print();

        System.out.println("<Cond>");
    }
}
