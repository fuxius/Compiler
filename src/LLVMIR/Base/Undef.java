package LLVMIR.Base;

import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 LLVM IR 中的未定义值 (undef)
 */
public class Undef extends Value {

    /**
     * 构造未定义值
     *
     * @param type 未定义值的类型
     */
    public Undef(LLVMType type) {
        super("undef", type);
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
    }

    /**
     * 返回 LLVM IR 格式的未定义值字符串
     *
     * @return "undef"
     */
    @Override
    public String toString() {
        return "undef";
    }
}
