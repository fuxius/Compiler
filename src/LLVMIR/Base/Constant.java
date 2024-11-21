package LLVMIR.Base;

import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 LLVM IR 中的常量值（不可变）
 */
public class Constant extends Value {
    private final int value; // 常量值

    /**
     * 构造整数常量，默认类型为 Int32
     *
     * @param value 常量值
     */
    public Constant(int value) {
        this(value, LLVMType.Int32);
    }

    /**
     * 构造指定类型的常量
     *
     * @param value 常量值
     * @param type  常量的 LLVM 类型
     */
    public Constant(int value, LLVMType type) {
        super(String.valueOf(value), type);
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        this.value = value;
    }

    /**
     * 获取常量的值
     *
     * @return 常量值
     */
    public int getValue() {
        return value;
    }
}
