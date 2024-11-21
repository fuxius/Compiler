package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;

public class Getchar extends Instruction {
    public Getchar(String name, BasicBlock parentBlock) {
        super(name, LLVMType.Int32, InstrType.GETCHAR, parentBlock);
    }
    public String toString(){
        return Name + " = call i32 @getchar()";
    }
}