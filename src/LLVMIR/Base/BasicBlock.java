package LLVMIR.Base;

import LLVMIR.Ins.Branch;
import LLVMIR.Ins.Ret;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Global.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示 LLVM IR 中的基本块，每个基本块是一个指令的序列
 */
public class BasicBlock extends User {
    private final List<Instruction> instrs; // 基本块中的指令
    private final Function parentFunc;      // 所属的函数

    /**
     * 构造基本块
     *
     * @param name       基本块的名称
     * @param parentFunc 所属的函数
     */
    public BasicBlock(String name, Function parentFunc) {
        super(name, LLVMType.funcType);

        if (parentFunc == null) {
            throw new IllegalArgumentException("Parent function cannot be null");
        }

        this.instrs = new ArrayList<>();
        this.parentFunc = parentFunc;
    }

    /**
     * 向基本块中添加指令
     *
     * @param instr 要添加的指令
     */
    public void addInstr(Instruction instr) {
        if (instr == null) {
            throw new IllegalArgumentException("Instruction cannot be null");
        }
        instrs.add(instr);
    }

    /**
     * 检查基本块是否以返回指令结束
     *
     * @return 如果最后一条指令是返回指令，返回 true
     */
    public boolean hasRet() {
        return !instrs.isEmpty() &&
                instrs.get(instrs.size() - 1).getInstrType() == Instruction.InstrType.RETURN;
    }

    /**
     * 获取基本块中的所有指令
     *
     * @return 指令列表
     */
    public List<Instruction> getInstrs() {
        return instrs;
    }

    /**
     * 获取所属的函数
     *
     * @return 所属的函数
     */
    public Function getParentFunc() {
        return parentFunc;
    }

    /**
     * 返回 LLVM IR 格式的基本块字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Name).append(":\n");
        for (Instruction instr : instrs) {
            sb.append("\t").append(instr).append("\n");
        }
        return sb.toString();
    }
    // 判断是否已经有跳转指令
    public boolean hasBr() {
        if (instrs.isEmpty()) return false;

        // 获取最后一条指令，判断是否是跳转指令
        Instruction lastInstr = instrs.get(instrs.size() - 1);
        return lastInstr instanceof Branch || lastInstr instanceof Ret;
    }
}
