package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.Base.Core.Value;

public class Ret extends Instruction {

    public Ret(Value exp, BasicBlock parent) {
        super(null, exp == null ? null : exp.getType(), InstrType.RETURN, parent);

        if (parent == null) {
            throw new IllegalArgumentException("Parent block cannot be null");
        }

        addOperand(exp);
    }

    @Override
    public String toString() {
        if (operands.get(0) == null) {
            return "ret void";
        }
        return String.format("ret %s %s", operands.get(0).getType(), operands.get(0).getName());
    }
}
