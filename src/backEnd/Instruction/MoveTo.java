package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class MoveTo extends AsmInstruction {
    private Register src;
    private Register dest;

    public MoveTo(Register src, Register dest) {
        this.src = src;
        this.dest = dest;
    }

    public Register getSrc() {
        return src;
    }

    public Register getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return "mov " + src + ", " + dest;
    }
}
