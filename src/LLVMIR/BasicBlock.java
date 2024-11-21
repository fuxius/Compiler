package LLVMIR;

import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.User;
import LLVMIR.Global.Function;
import java.util.ArrayList;
import java.util.List;

//BasicBlock 类表示一个基本块。每个基本块是一个指令的序列，必须以跳转、条件分支或返回指令结尾。基本块由 Instr 类表示的指令组成，并且属于某个 Function。
public class BasicBlock extends User {
    private List<Instruction> instrs;  // 基本块中的指令
    private Function parentFunc; // 所属的函数

    public BasicBlock(String name, Function parentFunc) {
        super(name, LLVMType.funcType);
        this.instrs = new ArrayList<>();
        this.parentFunc = parentFunc;
    }

    public void addInstr(Instruction instr) {
        instrs.add(instr);
    }
    public boolean hasRet() {
        if (instrs.size() > 0 && instrs.get(instrs.size() - 1).getInstrType() == Instruction.InstrType.RETURN) {
            return true;
        }
        return false;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Name).append(":\n");
        for (Instruction instr : instrs) {
            sb.append("\t").append(instr).append("\n");
        }
        return sb.toString();
    }
    public List<Instruction> getInstrs() {
        return instrs;
    }

    public Function getParentFunc() {
        return parentFunc;
    }
}
