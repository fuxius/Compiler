package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;

/**
 * 表示无条件跳转指令
 */
public class Jmp extends Instruction {

    /**
     * 构造跳转指令
     *
     * @param toBlock  跳转的目标基本块
     * @param parent   当前指令所属的基本块
     */
    public Jmp(BasicBlock toBlock, BasicBlock parent) {
        super(null, null, InstrType.JUMP, parent);

        if (toBlock == null || parent == null) {
            throw new IllegalArgumentException("Target block and parent block cannot be null");
        }

        addOperand(toBlock);
    }

    /**
     * 获取跳转目标块
     *
     * @return 跳转目标块
     */
    public BasicBlock getToBlock() {
        return (BasicBlock) operands.get(0);
    }

    /**
     * 返回 LLVM IR 格式的无条件跳转指令字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        return String.format("br label %%%s", operands.get(0).getName());
    }
}
