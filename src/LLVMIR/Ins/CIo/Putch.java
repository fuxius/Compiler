package LLVMIR.Ins.CIo;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Core.Value;

/**
 * 表示 LLVM IR 中的 putch 函数调用指令
 */
public class Putch extends Instruction {

    /**
     * 构造 Putch 指令
     *
     * @param value       要打印的整数值
     * @param parentBlock 当前指令所属的基本块
     */
    public Putch(Value value, BasicBlock parentBlock) {
        super(null, LLVMType.Void, InstrType.PUTCH, parentBlock);

        if (value == null || parentBlock == null) {
            throw new IllegalArgumentException("Value and parent block cannot be null");
        }

        addOperand(value);
    }

    /**
     * 返回 LLVM IR 格式的 putch 函数调用指令字符串
     *
     * @return 格式化的指令字符串
     */
    @Override
    public String toString() {
        return String.format("call void @putch(i32 %s)", operands.get(0).getName());
    }
}
