package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.Value;

public class Ret extends Instruction {
    public Ret(Value exp, BasicBlock parent){
        super(null,null,InstrType.RETURN,parent);
        addOperand(exp);
    }
    public String toString() {
        if (operands.get(0) == null) return "ret void";
        return "ret " + operands.get(0).getType() + " " + operands.get(0).getName();
    }
}