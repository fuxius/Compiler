package ast;

import java.util.List;

/**
 * 语句块节点
 * 对应文法：Block → '{' { BlockItem } '}'
 */
public class BlockNode {
    private List<BlockItemNode> blockItemNodes;

    public BlockNode(List<BlockItemNode> blockItemNodes) {
        this.blockItemNodes = blockItemNodes;
    }

    public void print() {
        System.out.println("LBRACE {");
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockItemNode.print();
        }
        System.out.println("RBRACE }");
        System.out.println("<Block>");
    }
}
