package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;

public class Jmp extends Instruction {
    public Jmp(BasicBlock toBlock,BasicBlock parent){
        super(null,null,InstrType.JUMP,parent);
        addOperand(toBlock);
    }
    public BasicBlock getToBlock(){
        return (BasicBlock) operands.get(0);
    }
    public String toString(){
        return "br label %" + operands.get(0).getName();
    }
}