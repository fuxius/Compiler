package ast;

import symbol.VariableSymbol;
import token.Token;

/**
 * 函数形参节点
 * 对应文法：FuncFParam → BType Ident ['[' ']']
 */
public class FuncFParamNode {
    private BTypeNode bTypeNode;
    private String ident;
    private boolean isArray; // 是否为数组参数
    private Token token;
    private VariableSymbol variableSymbol;

    public Token getToken() {
        return token;
    }

    public FuncFParamNode(BTypeNode bTypeNode, Token token, boolean isArray) {
        this.bTypeNode = bTypeNode;
        this.token = token;
        this.ident = token.getValue();
        this.isArray = isArray;
    }
    public VariableSymbol getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(VariableSymbol variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public String getIdent() {
        return ident;
    }

    public boolean isArray() {
        return isArray;
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
