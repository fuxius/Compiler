package ast;

/**
 * 语句块项节点
 * 对应文法：BlockItem → Decl | Stmt
 */
public class BlockItemNode {
    private DeclNode declNode;
    private StmtNode stmtNode;

    public BlockItemNode(DeclNode declNode) {
        this.declNode = declNode;
    }

    public BlockItemNode(StmtNode stmtNode) {
        this.stmtNode = stmtNode;
    }

    public void print() {
        if (declNode != null) {
            declNode.print();
        } else if (stmtNode != null) {
            stmtNode.print();
        }
        // 不需要输出 <BlockItem>
    }
}
