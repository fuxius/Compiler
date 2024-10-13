package ast;

/**
 * 相等性表达式节点
 * 对应文法：EqExp → RelExp | EqExp ('==' | '!=') RelExp
 */
public class EqExpNode {
    private EqExpNode eqExpNode;
    private String op; // '==' 或 '!='
    private RelExpNode relExpNode;
    private RelExpNode singleRelExpNode;

    // 单个 RelExp
    public EqExpNode(RelExpNode singleRelExpNode) {
        this.singleRelExpNode = singleRelExpNode;
    }

    // EqExp ('==' | '!=') RelExp
    public EqExpNode(EqExpNode eqExpNode, String op, RelExpNode relExpNode) {
        this.eqExpNode = eqExpNode;
        this.op = op;
        this.relExpNode = relExpNode;
    }

    public void print() {
        if (singleRelExpNode != null) {
            singleRelExpNode.print();
        } else {
            eqExpNode.print();
            if (op.equals("==")) {
                System.out.println("EQL ==");
            } else if (op.equals("!=")) {
                System.out.println("NEQ !=");
            }
            relExpNode.print();
        }
        System.out.println("<EqExp>");
    }
}
