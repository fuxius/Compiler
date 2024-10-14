package symbol;

import java.util.List;

/**
 * 函数符号类，表示函数信息。
 */
public class FunctionSymbol extends Symbol {
    private String returnType;                  // 返回类型（Int、Char、Void）
    private List<VariableSymbol> parameters;    // 参数列表

    public FunctionSymbol(String name, String typeName, int scopeLevel, String returnType, List<VariableSymbol> parameters) {
        super(name, typeName, scopeLevel);
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<VariableSymbol> getParameters() {
        return parameters;
    }

    public FunctionSymbol(String name, int scopeLevel, String returnType, List<VariableSymbol> parameters) {
        super(name, null, scopeLevel);
        this.returnType = returnType;
        this.parameters = parameters;
        this.typeName = generateTypeName();
    }

    private String generateTypeName() {
        StringBuilder typeNameBuilder = new StringBuilder();
        typeNameBuilder.append(returnType);
        typeNameBuilder.append("Func");
        return typeNameBuilder.toString();
    }
}
