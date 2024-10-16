package ast;

import token.Token;

import java.util.List;

/**
 * 语句块节点
 * 对应文法：Block → '{' { BlockItem } '}'
 */
public class BlockNode {
    private List<BlockItemNode> blockItemNodes;
    private Token token;

    public Token getToken() {
        return token;
    }

    public BlockNode(List<BlockItemNode> blockItemNodes, Token token) {
        this.token = token;
        this.blockItemNodes = blockItemNodes;
    }

    public List<BlockItemNode> getBlockItemNodes() {
        return blockItemNodes;
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
