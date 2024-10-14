package ast;

import token.Token;

/**
 * 变量定义节点
 * 对应文法：
 * VarDef → Ident [ '[' ConstExp ']' ]
 *          | Ident [ '[' ConstExp ']' ] '=' InitVal
 */
public class VarDefNode {
    private String ident; // 标识符名称
    private Token token;
    private ConstExpNode constExpNode; // 可选的数组大小
    private InitValNode initValNode; // 可选的初始值

    public VarDefNode(Token token, ConstExpNode constExpNode, InitValNode initValNode) {
        this.token = token;
        this.ident = token.getValue();
        this.constExpNode = constExpNode;
        this.initValNode = initValNode;
    }

    public String getIdent() {
        return ident;
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public InitValNode getInitValNode() {
        return initValNode;
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
