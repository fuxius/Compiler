package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Move extends Instruction {

    public Move(Value to, Value from, BasicBlock parentBlock) {
        super(null, LLVMType.Void,InstrType.MOVE, parentBlock);
        addOperand(to);
        addOperand(from);
    }

    public Value getFrom() {
        return operands.get(1);
    }

    public Value getTo() {
        return operands.get(0);
    }
    public void setFrom(Value value){
        if(value!=null) value.addUser(this);
        operands.set(1,value);
    }
    public String toString(){
        return "move "+getTo().getName()+","+getFrom().getName();
    }
}