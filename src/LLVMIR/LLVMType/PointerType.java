package LLVMIR.LLVMType;

public class PointerType extends LLVMType {
    private LLVMType pointedType; // 指针指向的类型

    public PointerType(LLVMType pointedType) {
        this.pointedType = pointedType;
    }

    public LLVMType getPointedType() {
        return pointedType;
    }
    public String toString(){
        return pointedType+"*";
    }
}
