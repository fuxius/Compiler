package ast;

import symbol.VariableSymbol;
import token.Token;

import java.util.List;

/**
 * 常量定义节点
 * 对应文法：ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
 */
public class ConstDefNode {
    private String ident; // 标识符名称
    private Token token;
    private ConstExpNode constExpNode; // 数组维度列表
    private ConstInitValNode constInitValNode; // 常量初值
    private VariableSymbol variableSymbol; //符号表项

    public VariableSymbol getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(VariableSymbol variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public Token getToken() {
        return token;
    }

    public ConstDefNode(Token token,  ConstExpNode constExpNode, ConstInitValNode constInitValNode) {
        this.token = token;
        this.ident = token.getValue();
        this.constExpNode = constExpNode;
        this.constInitValNode = constInitValNode;
    }

    public String getIdent() {
        return ident;
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public ConstInitValNode getConstInitValNode() {
        return constInitValNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);

            System.out.println("LBRACK [");
            constExpNode.print();
            System.out.println("RBRACK ]");

        System.out.println("ASSIGN =");
        constInitValNode.print();
        System.out.println("<ConstDef>");
    }

}
