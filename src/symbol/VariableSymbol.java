package symbol;

/**
 * 变量符号类，包含变量的特有信息
 */
public class VariableSymbol extends Symbol {
    private boolean isConst; // 是否为常量
    private int dimension;   // 数组维度，0表示非数组

    public VariableSymbol(String name, int scopeLevel, String typeName, boolean isConst, int dimension) {
        super(name, scopeLevel, typeName);
        this.isConst = isConst;
        this.dimension = dimension;
    }

    public boolean isConst() {
        return isConst;
    }

    public int getDimension() {
        return dimension;
    }
}
