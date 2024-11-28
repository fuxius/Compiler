package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

/**
 * 表示 LLVM 中的分支指令
 */
public class Branch extends Instruction {
    private final boolean isConditional; // 是否为条件分支

    /**
     * 构造条件分支指令
     *
     * @param condition 条件值
     * @param thenBlock 条件为 true 时跳转的目标块
     * @param elseBlock 条件为 false 时跳转的目标块
     * @param parent    所属基本块
     */
    public Branch(Value condition, BasicBlock thenBlock, BasicBlock elseBlock, BasicBlock parent) {
        super(null, LLVMType.Void, InstrType.BRANCH, parent);

        if (condition == null || thenBlock == null || elseBlock == null) {
            throw new IllegalArgumentException("Condition and target blocks cannot be null for conditional branch");
        }

        this.isConditional = true;
        addOperand(condition);
        addOperand(thenBlock);
        addOperand(elseBlock);
    }

    /**
     * 构造无条件分支指令
     *
     * @param targetBlock 跳转的目标块
     * @param parent      所属基本块
     */
    public Branch(BasicBlock targetBlock, BasicBlock parent) {
        super(null, LLVMType.Void, InstrType.BRANCH, parent);

        if (targetBlock == null) {
            throw new IllegalArgumentException("Target block cannot be null for unconditional branch");
        }

        this.isConditional = false;
        addOperand(targetBlock);
    }

    /**
     * 获取条件分支的 "then" 块
     *
     * @return "then" 块
     * @throws IllegalStateException 如果不是条件分支
     */
    public BasicBlock getThenBlock() {
        if (!isConditional) {
            throw new IllegalStateException("Cannot get 'then' block for unconditional branch");
        }
        return (BasicBlock) operands.get(1);
    }

    /**
     * 获取条件分支的 "else" 块
     *
     * @return "else" 块
     * @throws IllegalStateException 如果不是条件分支
     */
    public BasicBlock getElseBlock() {
        if (!isConditional) {
            throw new IllegalStateException("Cannot get 'else' block for unconditional branch");
        }
        return (BasicBlock) operands.get(2);
    }

    /**
     * 获取无条件分支的目标块
     *
     * @return 目标块
     * @throws IllegalStateException 如果是条件分支
     */
    public BasicBlock getTargetBlock() {
        if (isConditional) {
            throw new IllegalStateException("Cannot get target block for conditional branch");
        }
        return (BasicBlock) operands.get(0);
    }

    /**
     * 返回 LLVM IR 格式的分支指令字符串
     *
     * @return 格式化的分支指令字符串
     */
    @Override
    public String toString() {
        if (isConditional) {
            return String.format("br i1 %s, label %%%s, label %%%s",
                    operands.get(0).getName(),
                    operands.get(1).getName(),
                    operands.get(2).getName());
        } else {
            return String.format("br label %%%s", operands.get(0).getName());
        }
    }

    public boolean isConditional() {
        return isConditional;
    }
}
