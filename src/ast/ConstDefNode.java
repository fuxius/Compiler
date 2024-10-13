package ast;

/**
 * 常量定义节点
 * 对应文法：ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
 */
public class ConstDefNode {
    private String ident; // 标识符名称
    private ConstExpNode constExpNode; // 可选的数组大小
    private ConstInitValNode constInitValNode; // 常量初值

    public ConstDefNode(String ident, ConstExpNode constExpNode, ConstInitValNode constInitValNode) {
        this.ident = ident;
        this.constExpNode = constExpNode;
        this.constInitValNode = constInitValNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);
        if (constExpNode != null) {
            System.out.println("LBRACK [");
            constExpNode.print();
            System.out.println("RBRACK ]");
        }
        System.out.println("ASSIGN =");
        constInitValNode.print();
        System.out.println("<ConstDef>");

    }
}
