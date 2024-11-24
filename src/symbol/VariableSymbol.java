package symbol;

import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

import java.util.ArrayList;

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
    private int length = 0; // 数组长度，仅对数组有效

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }


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

    public LLVMType getLLVMType() {
        return LLVMType;
    }

    public void setLLVMType(LLVMType LLVMType) {
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
