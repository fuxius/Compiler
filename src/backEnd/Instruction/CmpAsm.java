package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class CmpAsm extends AsmInstruction {
    public enum CmpOp {
        seq, sne, slt, sgt, sle, sge
    }
    private CmpOp op;
    private Register rd, rs, rt;
    private int imm;
    // 比较运算
    public CmpAsm(CmpOp op, Register rd, Register rs, Register rt) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }
    // 比较运算
    public CmpAsm(CmpOp op, Register rd, Register rs, int imm) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    public CmpOp getOp() {
        return op;
    }

    public Register getRd() {
        return rd;
    }

    public Register getRs() {
        return rs;
    }

    public Register getRt() {
        return rt;
    }

    public int getImm() {
        return imm;
    }
}
