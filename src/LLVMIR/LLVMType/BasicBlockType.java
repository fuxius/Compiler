package LLVMIR.LLVMType;

/**
 * 表示 LLVM IR 中的基本块类型
 *
 * 当前不包含其他属性，但可以在需要时扩展。
 */
public class BasicBlockType extends LLVMType {

    /**
     * 返回 LLVM IR 格式的基本块类型字符串
     *
     * @return 类型名称
     */
    @Override
    public String toString() {
        return "BasicBlockType";
    }
}
