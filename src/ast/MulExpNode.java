package ast;

/**
 * 乘除模表达式节点
 * 对应文法：MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
 */
public class MulExpNode {
    private MulExpNode mulExpNode;
    private String op; // '*', '/', '%'
    private UnaryExpNode unaryExpNode;
    private UnaryExpNode singleUnaryExpNode;

    // 单个 UnaryExp
    public MulExpNode(UnaryExpNode singleUnaryExpNode) {
        this.singleUnaryExpNode = singleUnaryExpNode;
    }

    // MulExp ('*' | '/' | '%') UnaryExp
    public MulExpNode(MulExpNode mulExpNode, String op, UnaryExpNode unaryExpNode) {
        this.mulExpNode = mulExpNode;
        this.op = op;
        this.unaryExpNode = unaryExpNode;
    }

    public void print() {
        if (singleUnaryExpNode != null) {
            singleUnaryExpNode.print();
        } else {
            mulExpNode.print();
            if (op.equals("*")) {
                System.out.println("MULT *");
            } else if (op.equals("/")) {
                System.out.println("DIV /");
            } else if (op.equals("%")) {
                System.out.println("MOD %");
            }
            unaryExpNode.print();
        }
        System.out.println("<MulExp>");
    }
}
