package ast;

/**
 * 变量定义节点
 * 对应文法：
 * VarDef → Ident [ '[' ConstExp ']' ]
 *          | Ident [ '[' ConstExp ']' ] '=' InitVal
 */
public class VarDefNode {
    private String ident; // 标识符名称
    private ConstExpNode constExpNode; // 可选的数组大小
    private InitValNode initValNode; // 可选的初始值

    public VarDefNode(String ident, ConstExpNode constExpNode, InitValNode initValNode) {
        this.ident = ident;
        this.constExpNode = constExpNode;
        this.initValNode = initValNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);
        if (constExpNode != null) {
            System.out.println("LBRACK [");
            constExpNode.print();
            System.out.println("RBRACK ]");
        }
        if (initValNode != null) {
            System.out.println("ASSIGN =");
            initValNode.print();
        }
        System.out.println("<VarDef>");

    }
}
