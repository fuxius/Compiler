package LLVMIR.LLVMType;

/**
 * 表示 LLVM IR 中的函数类型
 */
public class FuncType extends LLVMType {

    /**
     * 构造函数类型
     */
    public FuncType() {
        // 当前无需额外初始化逻辑，保留扩展空间
    }

    /**
     * 返回 LLVM IR 格式的函数类型字符串
     *
     * @return 类型字符串
     */
    @Override
    public String toString() {
        return "FuncType"; // 默认字符串表示，保留未来扩展可能性
    }
}
