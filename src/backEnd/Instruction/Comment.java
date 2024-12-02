package backEnd.Instruction;

import LLVMIR.Base.Instruction;
import backEnd.Base.AsmInstruction;

public class Comment extends AsmInstruction {
    private Instruction llvmInstr;

    public Comment(Instruction llvmInstr) {
        this.llvmInstr = llvmInstr;
    }
    public String toString(){
        return "\n"+"\t\t#"+llvmInstr.toString();
    }
}