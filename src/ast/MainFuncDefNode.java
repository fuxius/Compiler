package ast;

import token.Token;

/**
 * 主函数定义节点
 * 对应文法：MainFuncDef → 'int' 'main' '(' ')' Block
 */
public class MainFuncDefNode {
    private BlockNode blockNode;
    private Token token;

    public MainFuncDefNode(BlockNode blockNode,Token token) {
        this.token = token;
        this.blockNode = blockNode;
    }

    public Token getToken() {
        return token;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public void print() {
        System.out.println("INTTK int");
        System.out.println("MAINTK main");
        System.out.println("LPARENT (");
        System.out.println("RPARENT )");
        blockNode.print();
        System.out.println("<MainFuncDef>");
    }
}
