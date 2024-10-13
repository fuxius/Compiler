package ast;

/**
 * 函数定义节点
 * 对应文法：FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
 */
public class FuncDefNode {
    private FuncTypeNode funcTypeNode;
    private String ident;
    private FuncFParamsNode funcFParamsNode; // 可选
    private BlockNode blockNode;

    public FuncDefNode(FuncTypeNode funcTypeNode, String ident, FuncFParamsNode funcFParamsNode, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.ident = ident;
        this.funcFParamsNode = funcFParamsNode;
        this.blockNode = blockNode;
    }

    public void print() {
        funcTypeNode.print();
        System.out.println("IDENFR " + ident);
        System.out.println("LPARENT (");
        if (funcFParamsNode != null) {
            funcFParamsNode.print();
        }
        System.out.println("RPARENT )");
        blockNode.print();
        System.out.println("<FuncDef>");
    }
}
