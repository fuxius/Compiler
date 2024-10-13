package ast;

/**
 * For语句节点
 * 对应文法：ForStmt → LVal '=' Exp
 */
public class ForStmtNode {
    private LValNode lValNode;
    private ExpNode expNode;

    public ForStmtNode(LValNode lValNode, ExpNode expNode) {
        this.lValNode = lValNode;
        this.expNode = expNode;
    }

    public void print() {
        lValNode.print();
        System.out.println("ASSIGN =");
        expNode.print();

        System.out.println("<ForStmt>");
    }
}
