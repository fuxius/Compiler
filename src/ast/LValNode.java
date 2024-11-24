package ast;

import symbol.Symbol;
import symbol.SymbolTable;
import symbol.VariableSymbol;
import token.Token;

import java.util.List;

/**
 * 左值表达式节点
 * 对应文法：LVal → Ident ['[' Exp ']']
 */
public class LValNode {
    private String ident;
    private ExpNode expNode; // 可选的数组下标

    private Token token;

    private VariableSymbol variableSymbol; //符号表项


    public Token getToken() {
        return token;
    }

    public LValNode(Token token, ExpNode expNode) {
        this.token = token;
        this.ident = token.getValue();
        this.expNode = expNode;
    }

    public VariableSymbol getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(VariableSymbol variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public String getIdent() {
        return ident;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);
        if (expNode != null) {
            System.out.println("LBRACK [");
            expNode.print();
            System.out.println("RBRACK ]");
        }
        System.out.println("<LVal>");
    }
    /**
     * Evaluate 方法计算 LVal 的值。
     * 对于标量，直接返回其值；对于数组，返回指定下标位置的值。
     * 假设所有数据都是正确的，不进行错误检查。
     * @return 计算后的整数值。
     */
    public int evaluate() {
        // 1. 从符号表查找标识符
        VariableSymbol varSymbol = (VariableSymbol) SymbolTable.getInstance().lookup(ident);

        // 2. 常量或变量的处理逻辑
        if (varSymbol.getDimension() == 0) {
            // 标量情况，直接返回初始值
            return varSymbol.getInitialValues().get(0);
        } else {
            // 数组情况，计算下标并返回对应的初始值
            int index = expNode.evaluate();  // 计算数组下标
            return varSymbol.getInitialValues().get(index);
        }
    }


}
