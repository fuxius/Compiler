package symbol;

import symbol.Symbol;

public class VariableSymbol extends Symbol {
    private String baseType; // 基本类型："int" 或 "char"
    private int dimension;   // 维数
    private boolean isConst; // 是否为常量

    public VariableSymbol(String name, int scopeLevel, String typeName, boolean isConst, int dimension, String baseType) {
        super(name, scopeLevel, typeName);
        this.isConst = isConst;
        this.dimension = dimension;
        this.baseType = baseType;
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
