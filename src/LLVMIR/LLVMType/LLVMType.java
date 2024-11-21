package LLVMIR.LLVMType;

public class LLVMType {
    public static BasicBlockType basicBlockType = new BasicBlockType();
    public static FuncType funcType = new FuncType();
    public static BType Void = new BType(0);
    public static BType Int1 = new BType(1);
    public static BType Int8 = new BType(8);
    public static BType Int32 = new BType(32);

    public boolean isArray() {
        return this instanceof ArrayType;
    }

    public boolean isInt1() {
        return this == BType.Int1;
    }
    public boolean isInt8() {
        return this == BType.Int8;
    }
    public boolean isInt32() {
        return this == BType.Int32;
    }

    public boolean isVoid() {
        return this == BType.Void;
    }

    public boolean isPointer() {
        return this instanceof PointerType;
    }


    public boolean isFUNCTION() {
        return this  instanceof FuncType;
    }
    public boolean isBiggerThan(LLVMType otherType) {
        if (this.equals(otherType) || this == LLVMType.Void) {
            return false; // 相等的类型，不需要转换
        }
        if (this == LLVMType.Int32 && otherType == LLVMType.Int8) {
            return true; // Int32 比 Int8 大，需要截断
        }
        if (this == LLVMType.Int8 && otherType == LLVMType.Int32) {
            return false; // Int8 比 Int32 小，需要扩展
        }
        throw new IllegalArgumentException("Type comparison not supported for: " + this + " and " + otherType);
    }


}
