package ast;

import java.util.List;

/**
 * 常量声明节点
 * 对应文法：ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
 */
public class ConstDeclNode {
    // 基本类型
    private BTypeNode bTypeNode;
    // 常量定义列表
    private List<ConstDefNode> constDefNodes;

    public ConstDeclNode(BTypeNode bTypeNode, List<ConstDefNode> constDefNodes) {
        this.bTypeNode = bTypeNode;
        this.constDefNodes = constDefNodes;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public List<ConstDefNode> getConstDefNodes() {
        return constDefNodes;
    }

    public void print() {
        // 输出 'const' 关键字
        System.out.println("CONSTTK const");
        bTypeNode.print();
        for (ConstDefNode constDefNode : constDefNodes) {
            constDefNode.print();
        }
        // 输出 ';'
        System.out.println("SEMICN ;");
        System.out.println("<ConstDecl>");
    }
}
