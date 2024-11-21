package LLVMIR.Base;

import LLVMIR.LLVMType.LLVMType;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示 LLVM IR 中的初始值
 */
public class Initial {
    private final LLVMType type;           // 初始值的类型
    private final List<Integer> values;   // 初始值的列表

    /**
     * 构造初始值对象
     *
     * @param type   初始值的类型
     * @param values 初始值列表
     */
    public Initial(LLVMType type, List<Integer> values) {
        if (type == null || values == null) {
            throw new IllegalArgumentException("Type and values cannot be null");
        }
        this.type = type;
        this.values = new ArrayList<>(values); // 确保列表的不可变性
    }

    /**
     * 获取初始值的类型
     *
     * @return 类型
     */
    public LLVMType getType() {
        return type;
    }

    /**
     * 获取初始值的列表
     *
     * @return 初始值列表
     */
    public List<Integer> getValues() {
        return new ArrayList<>(values); // 返回副本以保证不可变性
    }
}
