package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

/**
 * 表示 LLVM IR 中的零扩展 (zext) 指令
 */
public class Zext extends Instruction {

    /**
     * 构造 Zext 指令
     *
     * @param name       指令名称
     * @param value      要扩展的值
     * @param parentBlock 当前指令所属的基本块
     * @param aimType    扩展后的目标类型
     */
    public Zext(String name, Value value, BasicBlock parentBlock, LLVMType aimType) {
        super(name, aimType, InstrType.ZEXT, parentBlock);

        if (name == null || value == null || parentBlock == null || aimType == null) {
            throw new IllegalArgumentException("Name, value, parent block, and aimType cannot be null");
        }

        addOperand(value);
    }

    @Override
    public String toString() {
        return String.format("%s = zext %s %s to %s",
                Name,
                operands.get(0).getType(),
                operands.get(0).getName(),
                type);
    }
}
