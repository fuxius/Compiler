package ast;

import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
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
    public void initializeVariableSymbolForLLVM() {
        // 确定基本类型和数组长度
        String baseType = variableSymbol.getBaseType();
        int length = constExpNode != null ? constExpNode.evaluate() : 0;
        variableSymbol.setLength(length);

        // 计算 LLVM 类型
        LLVMType llvmType = determineLLVMType(baseType, length);
        variableSymbol.setLLVMType(llvmType);

        // 初始化值处理
        variableSymbol.setZeroInitialized(false);
        if (variableSymbol.isGlobal()) {
            // 全局常量
            if (constInitValNode != null) {
                // 设置初始值
                variableSymbol.setInitialValues(constInitValNode.evaluate());
                variableSymbol.setZeroInitialized(constInitValNode.isZero());
            } else {
                // 未初始化全局常量默认置零
                variableSymbol.setZeroInitialized(true);
            }
        } else {
            // 局部常量
            if (constInitValNode != null) {
                variableSymbol.setInitialValues(constInitValNode.evaluate());
            }
        }
    }

    // 辅助方法：确定 LLVM 类型
    private LLVMType determineLLVMType(String baseType, int length) {
        if (constInitValNode.isStringConst()) {
            // 字符串常量
            return new ArrayType(LLVMType.Int8, length);
        } else if (length == 0) {
            // 标量类型
            return baseType.equals("int") ? LLVMType.Int32 : LLVMType.Int8;
        } else {
            // 数组类型
            LLVMType elementType = baseType.equals("int") ? LLVMType.Int32 : LLVMType.Int8;
            return new ArrayType(elementType, length);
        }
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
