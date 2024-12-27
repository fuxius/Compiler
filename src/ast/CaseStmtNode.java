package ast;

public class CaseStmtNode {
    private ConstExpNode constExpNode;
    private StmtNode stmtNode;

    public CaseStmtNode(ConstExpNode constExpNode, StmtNode stmtNode) {
        this.constExpNode = constExpNode;
        this.stmtNode = stmtNode;
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public void setConstExpNode(ConstExpNode constExpNode) {
        this.constExpNode = constExpNode;
    }

    public StmtNode getStmtNode() {
        return stmtNode;
    }

    public void setStmtNode(StmtNode stmtNode) {
        this.stmtNode = stmtNode;
    }
}
