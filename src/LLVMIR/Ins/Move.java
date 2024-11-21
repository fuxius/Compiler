package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

/**
 * 表示 LLVM IR 中的移动指令 (move)
 */
public class Move extends Instruction {

    /**
     * 构造 Move 指令
     *
     * @param to          目标值
     * @param from        源值
     * @param parentBlock 当前指令所属基本块
     */
    public Move(Value to, Value from, BasicBlock parentBlock) {
        super(null, LLVMType.Void, InstrType.MOVE, parentBlock);

        if (to == null || from == null) {
            throw new IllegalArgumentException("Both 'to' and 'from' values cannot be null");
        }

        addOperand(to);
        addOperand(from);
    }

    /**
     * 获取源操作数
     *
     * @return 源操作数
     */
    public Value getFrom() {
        return operands.get(1);
    }

    /**
     * 获取目标操作数
     *
     * @return 目标操作数
     */
    public Value getTo() {
        return operands.get(0);
    }

    /**
     * 设置源操作数
     *
     * @param value 新的源操作数
     */
    public void setFrom(Value value) {
        if (value == null) {
            throw new IllegalArgumentException("Source operand cannot be null");
        }

        value.addUser(this);
        operands.set(1, value);
    }

    /**
     * 返回 LLVM IR 格式的移动指令字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        return String.format("move %s, %s", getTo().getName(), getFrom().getName());
    }
}
