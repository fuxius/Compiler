package LLVMIR.Ins.Mem;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Core.Value;

/**
 * 表示 LLVM IR 中的 store 指令
 */
public class Store extends Instruction {

    /**
     * 构造 Store 指令
     *
     * @param from   源值
     * @param to     目标指针
     * @param parent 当前指令所属的基本块
     */
    public Store(Value from, Value to, BasicBlock parent) {
        super(null, LLVMType.Void, InstrType.STORE, parent);

        if (from == null || to == null || parent == null) {
            throw new IllegalArgumentException("From, to, and parent block cannot be null");
        }

        addOperand(from);
        addOperand(to);
    }

    @Override
    public String toString() {
        Value from = operands.get(0);
        Value to = operands.get(1);
        return String.format("store %s %s, %s %s",
                from.getType(), from.getName(),
                to.getType(), to.getName());
    }

    /**
     * 获取源值
     *
     * @return 源值
     */
    public Value getFrom() {
        return operands.get(0);
    }

    /**
     * 获取目标指针
     *
     * @return 目标指针
     */
    public Value getTo() {
        return operands.get(1);
    }
}
