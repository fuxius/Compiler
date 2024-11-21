package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 getchar 函数调用指令
 */
public class Getchar extends Instruction {

    /**
     * 构造 Getchar 指令
     *
     * @param name        指令名称（返回值标识符）
     * @param parentBlock 所属基本块
     */
    public Getchar(String name, BasicBlock parentBlock) {
        super(name, LLVMType.Int32, InstrType.GETCHAR, parentBlock);

        if (name == null || parentBlock == null) {
            throw new IllegalArgumentException("Name and parent block cannot be null");
        }
    }

    /**
     * 返回 LLVM IR 格式的 getchar 调用指令字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        return String.format("%s = call i32 @getchar()", Name);
    }
}
