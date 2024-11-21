package LLVMIR.LLVMType;

/**
 * 表示 LLVM IR 中的指针类型
 */
public class PointerType extends LLVMType {
    private final LLVMType pointedType; // 指针指向的类型

    /**
     * 构造指针类型
     *
     * @param pointedType 指针指向的类型
     */
    public PointerType(LLVMType pointedType) {
        if (pointedType == null) {
            throw new IllegalArgumentException("Pointed type cannot be null");
        }
        this.pointedType = pointedType;
    }

    /**
     * 获取指针指向的类型
     *
     * @return 指向的类型
     */
    public LLVMType getPointedType() {
        return pointedType;
    }

    /**
     * 返回 LLVM IR 格式的指针类型字符串
     *
     * @return 类型字符串
     */
    @Override
    public String toString() {
        return String.format("%s*", pointedType);
    }
}
