package symbol;

/**
 * 符号基类，所有符号类型的父类。
 */
public abstract class Symbol {
    protected String name;          // 符号名称
    protected String typeName;      // 类型名称（如 Int、ConstInt、IntArray 等）
    protected int scopeLevel;       // 作用域序号

    public Symbol(String name, String typeName, int scopeLevel) {
        this.name = name;
        this.typeName = typeName;
        this.scopeLevel = scopeLevel;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }
}
