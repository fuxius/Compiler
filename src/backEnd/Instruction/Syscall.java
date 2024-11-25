package backEnd.Instruction;

import backEnd.Base.AsmInstruction;

public class Syscall extends AsmInstruction {
    public Syscall() {
    }

    @Override
    public String toString() {
        return "syscall";
    }
}
