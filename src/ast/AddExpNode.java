package ast;

/**
 * 加减表达式节点
 * 对应文法：AddExp → MulExp | AddExp ('+' | '−') MulExp
 */
public class AddExpNode {
    private AddExpNode addExpNode;
    private String op; // '+' 或 '-'
    private MulExpNode mulExpNode;
    private MulExpNode singleMulExpNode;

    // 单个 MulExp
    public AddExpNode(MulExpNode singleMulExpNode) {
        this.singleMulExpNode = singleMulExpNode;
    }

    // AddExp ('+' | '−') MulExp
    public AddExpNode(AddExpNode addExpNode, String op, MulExpNode mulExpNode) {
        this.addExpNode = addExpNode;
        this.op = op;
        this.mulExpNode = mulExpNode;
    }

    public void print() {
        if (singleMulExpNode != null) {
            singleMulExpNode.print();
        } else {
            addExpNode.print();
            if (op.equals("+")) {
                System.out.println("PLUS +");
            } else if (op.equals("-")) {
                System.out.println("MINU -");
            }
            mulExpNode.print();
        }
        System.out.println("<AddExp>");
    }
}
