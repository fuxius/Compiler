package symbol;

import LLVMIR.Global.GlobalVar;
import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;
import symbol.Symbol;

import java.util.ArrayList;

import static LLVMIR.LLVMType.LLVMType.*;

public class VariableSymbol extends Symbol {
    private String baseType; // 基本类型："int" 或 "char"
    private int dimension;   // 维数
    private boolean isConst; // 是否为常量
    private boolean isGlobal;
    private Value LLVMIR;
    private ArrayList<Integer> initialValues;

    public boolean isGlobal() {
        return isGlobal;
    }

    private boolean isZeroInitialized = false;


    private LLVMType LLVMType;


    public Value getLLVMIR() {
        return LLVMIR;
    }

    public void setLLVMIR(Value LLVMIR) {
        this.LLVMIR = LLVMIR;
    }

    public ArrayList<Integer> getInitialValues() {
        return initialValues;
    }

    public void setInitialValues(ArrayList<Integer> initialValues) {
        this.initialValues = initialValues;
    }

    public boolean isZeroInitialized() {
        return isZeroInitialized;
    }

    public void setZeroInitialized(boolean zeroInitialized) {
        isZeroInitialized = zeroInitialized;
    }

    public LLVMIR.LLVMType.LLVMType getLLVMType() {
        return LLVMType;
    }

    public void setLLVMType(LLVMIR.LLVMType.LLVMType LLVMType) {
        this.LLVMType = LLVMType;
    }

    public VariableSymbol(String name, int scopeLevel, String typeName, boolean isConst, int dimension, String baseType) {
        super(name, scopeLevel, typeName);
        this.isConst = isConst;
        this.dimension = dimension;
        this.baseType = baseType;
        if(scopeLevel == 1) {
            this.isGlobal = true;
        }else {
            this.isGlobal = false;
        }

    }


    public String getBaseType() {
        return baseType;
    }

    public int getDimension() {
        return dimension;
    }

    public boolean isConst() {
        return isConst;
    }
}
