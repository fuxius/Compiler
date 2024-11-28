package LLVMIR.LLVMType;

/**
 * 表示 LLVM IR 中的通用类型基类
 */
public class LLVMType {
    public static final BasicBlockType basicBlockType = new BasicBlockType();
    public static final FuncType funcType = new FuncType();
    public static final BType Void = new BType(0);
    public static final BType Int1 = new BType(1);
    public static final BType Int8 = new BType(8);
    public static final BType Int32 = new BType(32);

    public boolean isArray() {
        return this instanceof ArrayType;
    }

    public boolean isInt1() {
        return this == Int1;
    }

    public boolean isInt8() {
        // 添加对一维数组的判断
        if (this instanceof PointerType) {
            if (((PointerType) this).getPointedType() instanceof ArrayType) {
                return ((ArrayType) ((PointerType) this).getPointedType()).getElementType().isInt8();
            }
            return ((PointerType) this).getPointedType() == Int8;
        }
        return this == Int8;
    }

    public boolean isInt32() {
        // 添加对一维数组的判断
        if (this instanceof PointerType) {
            if (((PointerType) this).getPointedType() instanceof ArrayType) {
                return ((ArrayType) ((PointerType) this).getPointedType()).getElementType().isInt32();
            }
            return ((PointerType) this).getPointedType() == Int32;
        }
        return this == Int32;
    }

    public boolean isVoid() {
        return this == Void;
    }

    public boolean isPointer() {
        return this instanceof PointerType;
    }

    public boolean isFuncType() {
        return this instanceof FuncType;
    }

    /**
     * 比较当前类型与目标类型的大小关系
     *
     * @param otherType 目标类型
     * @return 当前类型是否大于目标类型
     */
    public boolean isBiggerThan(LLVMType otherType) {
        if (this == Int32 && otherType == Int8) {
            return true; // Int32 比 Int8 大
        }
        if (this == Int8 && otherType == Int32) {
            return false; // Int8 比 Int32 小
        }
        throw new IllegalArgumentException(
                String.format("Type comparison not supported between %s and %s", this, otherType));
    }
}
