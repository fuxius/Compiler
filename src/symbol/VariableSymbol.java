package symbol;

/**
 * 变量符号类，表示变量和常量，包括数组。
 */
public class VariableSymbol extends Symbol {
    private boolean isConst;    // 是否为常量
    private int dimension;      // 数组维度（0：变量，1：一维数组，2：二维数组）

    public VariableSymbol(String name, String baseType, int scopeLevel, boolean isConst, int dimension) {
        super(name, null, scopeLevel);
        this.isConst = isConst;
        this.dimension = dimension;
        this.typeName = generateTypeName(baseType);
    }

    private String generateTypeName(String baseType) {
        StringBuilder typeNameBuilder = new StringBuilder();
        if (isConst) {
            typeNameBuilder.append("Const");
        }
        typeNameBuilder.append(baseType);
        if (dimension == 1) {
            typeNameBuilder.append("Array");
        } else if (dimension == 2) {
            typeNameBuilder.append("Array"); // 如果需要区分二维数组，可以进一步修改
        }
        return typeNameBuilder.toString();
    }

    // 其他方法省略
}
