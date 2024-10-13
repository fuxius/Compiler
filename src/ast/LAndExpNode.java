package ast;

/**
 * 逻辑与表达式节点
 * 对应文法：LAndExp → EqExp | LAndExp '&&' EqExp
 */
public class LAndExpNode {
    private LAndExpNode lAndExpNode;
    private EqExpNode eqExpNode;
    private EqExpNode singleEqExpNode;

    // 单个 EqExp
    public LAndExpNode(EqExpNode singleEqExpNode) {
        this.singleEqExpNode = singleEqExpNode;
    }

    // LAndExp '&&' EqExp
    public LAndExpNode(LAndExpNode lAndExpNode, EqExpNode eqExpNode) {
        this.lAndExpNode = lAndExpNode;
        this.eqExpNode = eqExpNode;
    }

    public void print() {
        if (singleEqExpNode != null) {
            singleEqExpNode.print();
        } else {
            lAndExpNode.print();
            System.out.println("AND &&");
            eqExpNode.print();
        }
        System.out.println("<LAndExp>");
    }
}
