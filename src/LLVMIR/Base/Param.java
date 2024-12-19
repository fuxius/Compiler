package LLVMIR.Base;

import LLVMIR.Base.Core.Value;
import LLVMIR.Global.Function;
import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 LLVM IR 中的函数参数
 */
public class Param extends Value {
    private final Function parentFunction; // 所属函数

    /**
     * 构造函数参数对象
     *
     * @param name           参数名称
     * @param type           参数类型
     * @param parentFunction 参数所属的函数
     */
    public Param(String name, LLVMType type, Function parentFunction) {
        super(name, type);

        if (parentFunction == null) {
            throw new IllegalArgumentException("Parent function cannot be null");
        }

        this.parentFunction = parentFunction;
    }

    /**
     * 获取参数所属的函数
     *
     * @return 所属函数
     */
    public Function getParentFunction() {
        return parentFunction;
    }

    /**
     * 返回 LLVM IR 格式的参数字符串
     *
     * @return 格式化的参数字符串
     */
    @Override
    public String toString() {
        return String.format("%s %s", type, Name);
    }
}
