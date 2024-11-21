package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Putint extends Instruction {

    public Putint(Value value, BasicBlock parentBlock) {
        super(null, LLVMType.Void, InstrType.PUTINT, parentBlock);
        addOperand(value);
    }
    public String toString() {
        return "call void @putint(i32 " + operands.get(0).getName() + ")";
    }
}
