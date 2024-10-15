package symbol;

/**
 * 符号基类，包含符号的基本信息
 */
public class Symbol {
    protected String name;    // 符号名称
    protected int scopeLevel; // 作用域序号
    protected String typeName; // 类型名称

    public Symbol(String name, int scopeLevel, String typeName) {
        this.name = name;
        this.scopeLevel = scopeLevel;
        this.typeName = typeName;
    }

    public String getName() {
        return name;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }

    public String getTypeName() {
        return typeName;
    }
}
