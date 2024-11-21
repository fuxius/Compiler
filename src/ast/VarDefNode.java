package ast;

import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
import symbol.SymbolTable;
import symbol.VariableSymbol;
import token.Token;

import java.util.ArrayList;

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
    private VariableSymbol variableSymbol; //符号表项

    public VariableSymbol getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(VariableSymbol variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public VarDefNode(Token token, ConstExpNode constExpNode, InitValNode initValNode) {
        this.token = token;
        this.ident = token.getValue();
        this.constExpNode = constExpNode;
        this.initValNode = initValNode;
    }

    public String getIdent() {
        return ident;
    }

    public Token getToken() {
        return token;
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public InitValNode getInitValNode() {
        return initValNode;
    }

    public void initializeVariableSymbolForLLVM() {

        // 处理 LLVM 类型
        LLVMType llvmType = determineLLVMType(variableSymbol.getBaseType(), variableSymbol.getDimension());
        variableSymbol.setLLVMType(llvmType);

        variableSymbol.setZeroInitialized(false);
        if(variableSymbol.isGlobal()){
            //全局变量
            // 处理初始值（如果有）
            if (initValNode != null) {
                variableSymbol.setInitialValues(initValNode.evaluate());
                variableSymbol.setZeroInitialized(initValNode.isZero());
            } else  {
                variableSymbol.setZeroInitialized(true);
            }
        }

    }

    private LLVMType determineLLVMType(String baseType, int dimension) {
        if (dimension == 0) {
            return baseType.equals("int") ? LLVMType.Int32 : LLVMType.Int8;
        } else {
            LLVMType elementType = baseType.equals("int") ? LLVMType.Int32 : LLVMType.Int8;
            return new ArrayType(elementType, dimension);
        }
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
