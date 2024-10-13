package ast;

/**
 * 函数形参节点
 * 对应文法：FuncFParam → BType Ident ['[' ']']
 */
public class FuncFParamNode {
    private BTypeNode bTypeNode;
    private String ident;
    private boolean isArray; // 是否为数组参数

    public FuncFParamNode(BTypeNode bTypeNode, String ident, boolean isArray) {
        this.bTypeNode = bTypeNode;
        this.ident = ident;
        this.isArray = isArray;
    }

    public void print() {
        bTypeNode.print();
        System.out.println("IDENFR " + ident);
        if (isArray) {
            System.out.println("LBRACK [");
            System.out.println("RBRACK ]");
        }
        System.out.println("<FuncFParam>");
    }
}
