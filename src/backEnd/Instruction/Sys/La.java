package backEnd.Instruction.Sys;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class La extends AsmInstruction {
    //MIPS 汇编中的 la 指令（加载地址）
    private Register rd;
    private String label;

    public La(Register rd, String label) {
        this.rd = rd;
        this.label = label;
    }

    public Register getRd() {
        return rd;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "la " + rd + ", " + label;
    }


}
