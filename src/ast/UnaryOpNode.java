package ast;

/**
 * 单目运算符节点
 * 对应文法：UnaryOp → '+' | '−' | '!'
 */
public class UnaryOpNode {
    private String op; // '+', '-', '!'

    public UnaryOpNode(String op) {
        this.op = op;
    }

    public void print() {
        if (op.equals("+")) {
            System.out.println("PLUS +");
        } else if (op.equals("-")) {
            System.out.println("MINU -");
        } else if (op.equals("!")) {
            System.out.println("NOT !");
        }
        System.out.println("<UnaryOp>");
    }
}
