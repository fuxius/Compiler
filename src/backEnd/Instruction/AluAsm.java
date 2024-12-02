package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class AluAsm extends AsmInstruction {
    public enum AluOp {
        // 逻辑运算
        and, or, xor, nor, sll, srl, sra,
        // 算术运算
        addiu,addu, subu, mul, div,
        // 比较运算
        seq, sne, slt, sgt, sle, sge,
        // 移动
        mov
    }
    private AluOp op;
    private Register rd, rs, rt;
    private int imm;

    // 逻辑运算
    public AluAsm(AluOp op, Register rd, Register rs, Register rt) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    // 移动
    public AluAsm(AluOp op, Register rd, Register rs) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
    }

    // 算术运算
    public AluAsm(AluOp op, Register rd, Register rs, int imm) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    //rd:目的寄存器 rs:源寄存器 rt:源寄存器 imm:立即数
    public AluAsm(AluOp op, Register rd, Register rs, Register rt, int imm) {
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
        sb.append(op);
        sb.append(" ");
        sb.append(rd);
        sb.append(", ");
        sb.append(rs);
        if (rt != null) {
            sb.append(", ");
            sb.append(rt);
        } else  {
            sb.append(", ");
            sb.append(imm);
        }
        return sb.toString();
    }
}
