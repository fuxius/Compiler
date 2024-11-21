package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Constant;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class OffsetStore extends Instruction {
    public OffsetStore(String name, Value point, Constant offset, BasicBlock parent, Value value){
        super(name, LLVMType.Int32,InstrType.LOAD,parent);
        addOperand(value);
        addOperand(point);
        addOperand(offset);
    }
    public String toString() {
        Value from = operands.get(0);
        Value to = operands.get(1);
        int cons=((Constant)operands.get(2)).getValue();
        return "offsetStore " + from.getType() + " " + from.getName() + ", " + to.getType() + " " + '('+cons+')'+to.getName();
    }
}