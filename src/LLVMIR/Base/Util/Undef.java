package LLVMIR.Base.Util;

import LLVMIR.Base.Constant;
import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 LLVM IR 中的未定义值 (undef)
 */
public class Undef extends Constant {

    /**
     * 构造未定义值
     *
     */
    public Undef() {
        super(0);

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
