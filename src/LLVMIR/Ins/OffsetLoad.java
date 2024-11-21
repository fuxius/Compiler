package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Constant;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class OffsetLoad extends Instruction {
    public OffsetLoad(String name, Value point, Constant offset, BasicBlock parent){
        super(name, LLVMType.Int32,InstrType.LOAD,parent);
        addOperand(point);
        addOperand(offset);
    }
    public String toString(){
        return Name + " = offsetLoad " + type + ", " + operands.get(0).getType()
                + " " +"("+((Constant)operands.get(1)).getValue()+")" +operands.get(0).getName();
    }
}
