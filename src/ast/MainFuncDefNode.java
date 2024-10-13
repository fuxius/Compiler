package ast;

/**
 * 主函数定义节点
 * 对应文法：MainFuncDef → 'int' 'main' '(' ')' Block
 */
public class MainFuncDefNode {
    private BlockNode blockNode;

    public MainFuncDefNode(BlockNode blockNode) {
        this.blockNode = blockNode;
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
