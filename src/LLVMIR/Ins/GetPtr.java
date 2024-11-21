package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Value;

public class GetPtr extends Instruction {
    public GetPtr(String name, Value array, Value offset, BasicBlock parent){
        super(name, new PointerType(LLVMType.Int32),InstrType.GETPTR,parent);
        addOperand(array);
        addOperand(offset);
    }
    public String getGvnHash(){
        return "GetPtr "+operands.get(0).getName()+" "+operands.get(1).getName();
    }
    public String toString(){
        Value pointer = operands.get(0);
        Value offset = operands.get(1);
        PointerType pointerType = (PointerType) pointer.getType();
        LLVMType pointType = pointerType.getPointedType();
        if(pointType.isInt32()){
            return Name+" = getelementptr inbounds i32, i32* "+pointer.getName()+", i32 "+offset.getName();
        }
        else{
            return Name + " = getelementptr inbounds " + pointType + ", " + pointerType + " " +
                    pointer.getName() + ", i32 0, i32 " + offset.getName();
        }
    }
}
