package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Store extends Instruction {
    public Store( Value from, Value to, BasicBlock parent){
        super(null, LLVMType.Void,InstrType.STORE,parent);
        addOperand(from);
        addOperand(to);
    }
    @Override
    public String toString() {
        Value from = operands.get(0);
        Value to = operands.get(1);
        return "store " + from.getType() + " " + from.getName() + ", " + to.getType() + " " + to.getName();
    }
    public Value getFrom(){
        return operands.get(0);
    }
    public Value getTo(){
        return operands.get(1);
    }
}