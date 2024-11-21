package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Branch extends Instruction {
    public Branch(Value value, BasicBlock thenBlock, BasicBlock elseBlock, BasicBlock parent){
        super(null, LLVMType.Void,InstrType.BRANCH,parent);
        addOperand(value);
        addOperand(thenBlock);
        addOperand(elseBlock);
    }

    public BasicBlock getThenBlock(){
        return (BasicBlock) operands.get(1);
    }
    public BasicBlock getElseBlock(){
        return (BasicBlock) operands.get(2);
    }
    public String toString() {
        return "br i1 " + operands.get(0).getName() +
                ", label %" + operands.get(1).getName() + ", label %" + operands.get(2).getName();
    }
}
