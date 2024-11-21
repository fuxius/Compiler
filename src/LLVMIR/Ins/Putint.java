package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

public class Putint extends Instruction {

    public Putint(Value value, BasicBlock parentBlock) {
        super(null, LLVMType.Void, InstrType.PUTINT, parentBlock);

        if (value == null || parentBlock == null) {
            throw new IllegalArgumentException("Value and parent block cannot be null");
        }

        addOperand(value);
    }

    @Override
    public String toString() {
        return String.format("call void @putint(i32 %s)", operands.get(0).getName());
    }
}
