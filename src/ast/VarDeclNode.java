package ast;

import java.util.List;

/**
 * 变量声明节点
 * 对应文法：VarDecl → BType VarDef { ',' VarDef } ';'
 */
public class VarDeclNode {
    private BTypeNode bTypeNode;
    private List<VarDefNode> varDefNodes;

    public VarDeclNode(BTypeNode bTypeNode, List<VarDefNode> varDefNodes) {
        this.bTypeNode = bTypeNode;
        this.varDefNodes = varDefNodes;
    }

    public void print() {
        bTypeNode.print();
        for (VarDefNode varDefNode : varDefNodes) {
            varDefNode.print();
        }
        System.out.println("SEMICN ;");
        System.out.println("<VarDecl>");
    }
}
