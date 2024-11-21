package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Zext extends Instruction {
    private LLVMType aimType;

    public Zext(String name, Value value, BasicBlock parentBlock, LLVMType aimType) {
        super(name, aimType, InstrType.ZEXT, parentBlock);
        this.aimType = aimType;
        addOperand(value);
    }
    public String toString(){
        return Name+"= zext "+operands.get(0).getType()+" "+operands.get(0).getName()+" to "+aimType;
    }

    public LLVMType getAimType() {
        return aimType;
    }
}