package ast;

import symbol.FunctionSymbol;
import token.Token;

/**
 * 函数定义节点
 * 对应文法：FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
 */
public class FuncDefNode {
    private FuncTypeNode funcTypeNode;
    private String ident;
    private Token token;
    private FuncFParamsNode funcFParamsNode; // 可选
    private BlockNode blockNode;
    private FunctionSymbol functionSymbol;

    public FunctionSymbol getFunctionSymbol() {
        return functionSymbol;
    }

    public void setFunctionSymbol(FunctionSymbol functionSymbol) {
        this.functionSymbol = functionSymbol;
    }

    public Token getToken() {
        return token;
    }

    public FuncDefNode(FuncTypeNode funcTypeNode, Token token, FuncFParamsNode funcFParamsNode, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.token = token;
        this.ident = token.getValue();
        this.funcFParamsNode = funcFParamsNode;
        this.blockNode = blockNode;
    }

    public FuncTypeNode getFuncTypeNode() {
        return funcTypeNode;
    }

    public String getIdent() {
        return ident;
    }

    public FuncFParamsNode getFuncFParamsNode() {
        return funcFParamsNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
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
