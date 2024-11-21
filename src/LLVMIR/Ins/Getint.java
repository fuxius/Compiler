package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 getint 函数调用指令
 */
public class Getint extends Instruction {

    /**
     * 构造 Getint 指令
     *
     * @param name        指令名称（返回值标识符）
     * @param parentBlock 所属基本块
     */
    public Getint(String name, BasicBlock parentBlock) {
        super(name, LLVMType.Int32, InstrType.GETINT, parentBlock);

        if (name == null || parentBlock == null) {
            throw new IllegalArgumentException("Name and parent block cannot be null");
        }
    }

    /**
     * 返回 LLVM IR 格式的 getint 调用指令字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        return String.format("%s = call i32 @getint()", Name);
    }
}
