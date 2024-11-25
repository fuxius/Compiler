package backEnd.Instruction;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
public class MoveFrom extends AsmInstruction {
    //表示 MIPS 汇编中的 mfhi 和 mflo 指令，用于从特殊寄存器 hi 或 lo 中移动数据到目标寄存器
    public enum Type {
        MFHI, MFLO
    }
    private Type type;
    private Register dest;

    public MoveFrom(Type type, Register dest) {
        this.type = type;
        this.dest = dest;
    }

    public Type getType() {
        return type;
    }

    public Register getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return type.toString().toLowerCase() + " " + dest;
    }
}
