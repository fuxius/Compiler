package backEnd.Instruction.Sys;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class Li extends AsmInstruction {
    //表示 MIPS 汇编中的 li 指令（加载立即数）
    private Register rd;
    private int imm;

    public Li(Register rd, int imm) {
        this.rd = rd;
        this.imm = imm;
    }

    public Register getRd() {
        return rd;
    }

    public int getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return "li " + rd + ", " + imm;
    }

}
