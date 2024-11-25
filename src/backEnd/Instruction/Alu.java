package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class Alu extends AsmInstruction {
    public enum AluOp {
        // 逻辑运算
        and, or, xor, nor, sll, srl, sra,
        // 算术运算
        add, sub, mul, div, rem,
        // 比较运算
        seq, sne, slt, sgt, sle, sge,
        // 移动
        mov
    }
    private AluOp op;
    private Register rd, rs, rt;
    private int imm;

    // 逻辑运算
    public Alu(AluOp op, Register rd, Register rs, Register rt) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    // 移动
    public Alu(AluOp op, Register rd, Register rs) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
    }

    // 算术运算
    public Alu(AluOp op, Register rd, Register rs, int imm) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    // 比较运算
    public Alu(AluOp op, Register rd, Register rs, Register rt, int imm) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
        this.imm = imm;
    }

    public AluOp getOp() {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        sb.append(op);
        sb.append(" ");
        sb.append(rd);
        sb.append(", ");
        sb.append(rs);
        if (rt != null) {
            sb.append(", ");
            sb.append(rt);
        }
        if (imm != 0) {
            sb.append(", ");
            sb.append(imm);
        }
        sb.append("\n");
        return sb.toString();
    }
}
