package ast;

/**
 * 声明节点
 * 对应文法：Decl → ConstDecl | VarDecl
 */
public class DeclNode {
    private ConstDeclNode constDeclNode;
    private VarDeclNode varDeclNode;

    public DeclNode(ConstDeclNode constDeclNode) {
        this.constDeclNode = constDeclNode;
    }

    public DeclNode(VarDeclNode varDeclNode) {
        this.varDeclNode = varDeclNode;
    }

    public void print() {
        if (constDeclNode != null) {
            constDeclNode.print();
        } else if (varDeclNode != null) {
            varDeclNode.print();
        }
        // 不需要输出 <Decl>
    }
}
