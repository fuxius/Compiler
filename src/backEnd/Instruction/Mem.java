package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class Mem extends AsmInstruction {
    public enum MemOp {
        lb, lh, lw, sb, sh, sw
    }
    private MemOp op;
    private int offset;
    private Register rs;
    private Register rt;

    public Mem(MemOp op, int offset, Register rs, Register rt) {
        this.op = op;
        this.offset = offset;
        this.rs = rs;
        this.rt = rt;
    }

    public MemOp getOp() {
        return op;
    }

    public int getOffset() {
        return offset;
    }

    public Register getRs() {
        return rs;
    }

    public Register getRt() {
        return rt;
    }

    @Override
    public String toString() {
        return op + " " + rt + ", " + offset + "(" + rs + ")";
    }

}
