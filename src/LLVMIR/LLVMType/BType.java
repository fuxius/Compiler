package LLVMIR.LLVMType;

/**
 * 表示 LLVM IR 中的基本类型 (如 i32, i8, i1, void)
 */
public class BType extends LLVMType {
    private final int typeName; // 类型标识 (0: void, 32: i32, 8: i8, 1: i1)

    /**
     * 构造基本类型
     *
     * @param typeName 类型标识 (0, 32, 8, 1)
     */
    public BType(int typeName) {
        if (typeName != 0 && typeName != 32 && typeName != 8 && typeName != 1) {
            throw new IllegalArgumentException("Invalid typeName: " + typeName);
        }
        this.typeName = typeName;
    }

    /**
     * 获取类型标识
     *
     * @return 类型标识
     */
    public int getTypeName() {
        return typeName;
    }

    /**
     * 返回 LLVM IR 格式的基本类型字符串
     *
     * @return 类型字符串
     */
    @Override
    public String toString() {
        switch (typeName) {
            case 0:
                return "void";
            case 32:
                return "i32";
            case 8:
                return "i8";
            case 1:
                return "i1";
            default:
                throw new IllegalStateException("Unexpected typeName: " + typeName);
        }
    }
}
