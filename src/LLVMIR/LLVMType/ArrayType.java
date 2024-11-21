package LLVMIR.LLVMType;

/**
 * 表示 LLVM IR 中的数组类型
 */
public class ArrayType extends LLVMType {
    private final LLVMType elementType; // 数组元素类型
    private final int length;           // 数组长度,数组中元素的个数

    /**
     * 构造数组类型
     *
     * @param elementType 数组元素的类型
     * @param length      数组的长度
     */
    public ArrayType(LLVMType elementType, int length) {
        if (elementType == null) {
            throw new IllegalArgumentException("Element type cannot be null");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Array length cannot be negative");
        }

        this.elementType = elementType;
        this.length = length;
    }

    /**
     * 获取数组的元素类型
     *
     * @return 数组元素类型
     */
    public LLVMType getElementType() {
        return elementType;
    }

    /**
     * 获取数组的长度
     *
     * @return 数组长度
     */
    public int getLength() {
        return length;
    }

    /**
     * 返回 LLVM IR 格式的数组类型字符串
     *
     * @return 格式化的数组类型字符串
     */
    @Override
    public String toString() {
        return String.format("[%d x %s]", length, elementType);
    }
}
