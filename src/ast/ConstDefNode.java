package ast;

import token.Token;

import java.util.List;

/**
 * 常量定义节点
 * 对应文法：ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
 */
public class ConstDefNode {
    private String ident; // 标识符名称
    private Token token;
    private List<ConstExpNode> constExpNodes; // 数组维度列表
    private ConstInitValNode constInitValNode; // 常量初值

    public Token getToken() {
        return token;
    }

    public ConstDefNode(Token token,  List<ConstExpNode> constExpNodes, ConstInitValNode constInitValNode) {
        this.token = token;
        this.ident = token.getValue();
        this.constExpNodes = constExpNodes;
        this.constInitValNode = constInitValNode;
    }

    public String getIdent() {
        return ident;
    }

    public List<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public ConstInitValNode getConstInitValNode() {
        return constInitValNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);
        for (ConstExpNode constExpNode : constExpNodes) {
            System.out.println("LBRACK [");
            constExpNode.print();
            System.out.println("RBRACK ]");
        }
        System.out.println("ASSIGN =");
        constInitValNode.print();
        System.out.println("<ConstDef>");
    }

}
