package backEnd.Instruction;

import backEnd.Base.AsmInstruction;

public class Mem extends AsmInstruction {
    public enum MemOp {
        lb, lh, lw, sb, sh, sw
    }
    private MemOp op;
    private String label;
    private int imm;
    private String rs;
    private String rt;

    public Mem(MemOp op, String label, int imm, String rs) {
        this.op = op;
        this.label = label;
        this.imm = imm;
        this.rs = rs;
    }

    public Mem(MemOp op, String label, int imm, String rs, String rt) {
        this.op = op;
        this.label = label;
        this.imm = imm;
        this.rs = rs;
        this.rt = rt;
    }

    public MemOp getOp() {
        return op;
    }

    public String getLabel() {
        return label;
    }

    public int getImm() {
        return imm;
    }

    public String getRs() {
        return rs;
    }

    public String getRt() {
        return rt;
    }

    @Override
    public String toString() {
        return op + " " + label + " " + imm + "(" + rs + ")" + (rt == null ? "" : ", " + rt);
    }
}
