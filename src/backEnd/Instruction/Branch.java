package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;

public class Branch extends AsmInstruction {
    public enum BranchOp {
        beq, bne, blez, bgtz, bltz, bgez
    }
    private BranchOp op;
    private String label;
    private Register rs, rt;
    private int imm;

    // beq, bne
    public Branch(BranchOp op, Register rs, Register rt, String label) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.label = label;
    }
    // blez, bgtz, bltz, bgez
    public Branch(BranchOp op, Register rs, String label) {
        this.op = op;
        this.rs = rs;
        this.label = label;
    }
    // beq, bne
    public Branch(BranchOp op, Register rs, Register rt, int imm, String label) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.imm = imm;
        this.label = label;
    }
    // blez, bgtz, bltz, bgez
    public Branch(BranchOp op, Register rs, int imm, String label) {
        this.op = op;
        this.rs = rs;
        this.imm = imm;
        this.label = label;
    }

    public BranchOp getOp() {
        return op;
    }

    public Register getRs() {
        return rs;
    }

    public Register getRt() {
        return rt;
    }
}
