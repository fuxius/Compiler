package backEnd.Instruction;

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

    // beq, bne
    public BranchAsm(BranchOp op, Register rs, Register rt, String label) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.label = label;
    }
    // blt, bge, ble, bgt
    public BranchAsm(BranchOp op, Register rs, String label) {
        this.op = op;
        this.rs = rs;
        this.label = label;
    }
    //  beq, bne
    public BranchAsm(BranchOp op, Register rs, Register rt, int imm, String label) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.imm = imm;
        this.label = label;
    }
    // blt, bge, ble, bgt
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
        if (op == BranchOp.beq || op == BranchOp.bne) {
            // 如果有立即数
            if (imm != 0) {
                return op + "\t"  + rs + ", " + imm + ", " + label;
            }else return op + "\t" + rs + ", " + rt + ", " + label;
        } else {
            return op + "\t" + rs + ", " + imm + ", " + label;
        }
    }
}
