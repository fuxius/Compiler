package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Putch extends Instruction {

    public Putch(Value value, BasicBlock parentBlock) {
        super(null, LLVMType.Void, InstrType.PUTCH, parentBlock);
        addOperand(value);
    }
    public String toString() {
        return "call void @putch(i32 " + operands.get(0).getName() + ")";
    }
}
