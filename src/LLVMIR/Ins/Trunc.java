package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

/**
 * 表示 LLVM IR 中的截断 (trunc) 指令
 */
public class Trunc extends Instruction {

    /**
     * 构造 Trunc 指令
     *
     * @param name       指令名称
     * @param value      要截断的值
     * @param parent     当前指令所属的基本块
     * @param targetType 截断后的目标类型
     */
    public Trunc(String name, Value value, BasicBlock parent, LLVMType targetType) {
        super(name, targetType, InstrType.TRUNC, parent);

        if (name == null || value == null || parent == null || targetType == null) {
            throw new IllegalArgumentException("Name, value, parent block, and target type cannot be null");
        }

        addOperand(value);
    }

    @Override
    public String toString() {
        return String.format("%s = trunc %s %s to %s",
                Name,
                operands.get(0).getType(),
                operands.get(0).getName(),
                type);
    }
}
