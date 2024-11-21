package LLVMIR.LLVMType;

public class ArrayType extends LLVMType {
    private LLVMType elementType; // 数组元素类型
    private int length;           // 数组长度

    public ArrayType(LLVMType elementType, int length) {
        this.elementType = elementType;
        this.length = length;
    }

    public LLVMType getElementType() {
        return elementType;
    }

    public int getLength() {
        return length;
    }
    public String toString() {
        return "[" + length + " x " + elementType + "]";
    }
}

