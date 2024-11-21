package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;

public class Getint extends Instruction {
    public Getint(String name, BasicBlock parentBlock) {
        super(name, LLVMType.Int32, InstrType.GETINT, parentBlock);
    }
    public String toString(){
        return Name + " = call i32 @getint()";
    }
}
