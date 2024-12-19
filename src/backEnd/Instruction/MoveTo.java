package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class MoveTo extends AsmInstruction {
    public enum OP{
        hi,
        lo
    }
    private OP op;
    private Register dest;

    public MoveTo(OP op, Register dest) {
        this.op = op;
        this.dest = dest;
    }


    public OP getOp() {
        return op;
    }
    public Register getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return "mt" + op + ", " + dest;
    }
}
