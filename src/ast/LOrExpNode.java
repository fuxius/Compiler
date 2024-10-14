package ast;

/**
 * 逻辑或表达式节点
 * 对应文法：LOrExp → LAndExp | LOrExp '||' LAndExp
 */
public class LOrExpNode {
    private LOrExpNode lOrExpNode;
    private LAndExpNode lAndExpNode;
    private LAndExpNode singleLAndExpNode;

    // 单个 LAndExp
    public LOrExpNode(LAndExpNode singleLAndExpNode) {
        this.singleLAndExpNode = singleLAndExpNode;
    }

    // LOrExp '||' LAndExp
    public LOrExpNode(LOrExpNode lOrExpNode, LAndExpNode lAndExpNode) {
        this.lOrExpNode = lOrExpNode;
        this.lAndExpNode = lAndExpNode;
    }

    public LOrExpNode getlOrExpNode() {
        return lOrExpNode;
    }

    public LAndExpNode getlAndExpNode() {
        return lAndExpNode;
    }

    public LAndExpNode getSingleLAndExpNode() {
        return singleLAndExpNode;
    }

    public void print() {
        if (singleLAndExpNode != null) {
            singleLAndExpNode.print();
        } else {
            lOrExpNode.print();
            System.out.println("OR ||");
            lAndExpNode.print();
        }
        System.out.println("<LOrExp>");

    }
}
