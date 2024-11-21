package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Trunc extends Instruction {
    public Trunc( String name,Value value, BasicBlock parent,LLVMType targetType) {
        super(name, targetType, InstrType.TRUNC, parent);
        addOperand(value);
    }

    @Override
    public String toString() {
        return Name + " = trunc " + operands.get(0).getType() + " " + operands.get(0).getName() + " to " + type;
    }
}

