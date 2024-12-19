package backEnd.Instruction.Alu;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class AluAsm extends AsmInstruction {
    public enum AluOp {
        // 逻辑运算
        and, andi ,or, xor, nor, sll, srl, sra,
        // 算术运算
        addiu,addu, subu, mul, div,
        // 比较运算
        seq, sne, slt, sgt, sle, sge,lui,ori,
        // 移动
        mov
        ,madd,maddu,mult,multu
    }
    private AluOp op;
    private Register rd, rs, rt;
    private int imm;

    public AluAsm(AluOp op, Register rd, Register rs, Register rt) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    public AluAsm(AluOp op, Register rs, Register rt) {
        this.op = op;
        this.rt = rt;
        this.rs = rs;
    }

    // 算术运算
    public AluAsm(AluOp op, Register rd, Register rs, int imm) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
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

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(op);
//        sb.append(" ");
//        if(rd != null) {
//            sb.append(rd);
//            sb.append(", ");
//        }
//        sb.append(rs);
//        if (rt != null) {
//            sb.append(", ");
//            sb.append(rt);
//        } else {
//            sb.append(", ");
//            sb.append(imm);
//        }
//        return sb.toString();
//    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(op).append(" ");

        if (op == AluOp.div || op == AluOp.mult) {
            // 针对 div 和 mult 的特殊情况
            sb.append(rs).append(", ").append(rt);
        } else {
            if (op == AluOp.madd) {
                // 针对 madd 的特殊情况
                sb.append(rs).append(", ").append(rt).append("\n\t")
                        .append("mfhi ").append(rd);
            } else {
                // 普通情况
                if (rd != null) {
                    sb.append(rd).append(", ");
                }
                sb.append(rs).append(", ");

                if (rt != null) {
                    sb.append(rt);
                } else {
                    sb.append(imm); // 如果 rt 为空，输出立即数 imm
                }
            }
        }

        return sb.toString();
    }

}
