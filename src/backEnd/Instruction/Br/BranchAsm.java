package backEnd.Instruction.Br;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;

public class BranchAsm extends AsmInstruction {
    public enum BranchOp {
        beq, bne, blt, bge, ble, bgt
    }
    private BranchOp op;
    private String label;
    private Register rs, rt;
    private int imm;


    public BranchAsm(BranchOp op, Register rs, Register rt, String label) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.label = label;
    }
    public BranchAsm(BranchOp op, Register rs, int imm, String label) {
        this.op = op;
        this.rs = rs;
        this.imm = imm;
        this.label = label;
    }

    public String getLabel() {
        return label;
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

    // toString
    @Override
    public String toString() {
        if (rt == null) {
            return op + " " + rs + ", " + imm + ", " + label;
        } else {
            return op + " " + rs + ", " + rt + ", " + label;
        }
    }
}
