package ast;

public class DefaultStmtNode {
    private StmtNode stmtNode;

        public DefaultStmtNode(StmtNode stmtNode) {
            this.stmtNode = stmtNode;
        }

        public void print() {
            System.out.println("<DefaultStmt>");
            stmtNode.print();
            System.out.println("</DefaultStmt>");
        }

        public StmtNode getStmtNode() {
            return stmtNode;
        }
}
