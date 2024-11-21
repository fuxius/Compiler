package LLVMIR;

import LLVMIR.LLVMType.LLVMType;
//Instruction 类是所有指令的基类。每个指令表示一个操作，指令的类型由 InstructionType 枚举定义。每个指令类都将继承 Instruction 类，并定义具体的指令类型和功能。
public class Instruction extends User {
    public static enum InstrType {
        ALU, ALLOCA, BRANCH, CALL, GETPTR, ICMP, JUMP, LOAD, RETURN, STORE, ZEXT, GETINT, PUTSTR, PUTINT, PHI, MOVE,GETCHAR,PUTCH,TRUNC
    }

    private InstrType instrType;     // 指令类型
    private BasicBlock parentBlock;  // 指令所属的基本块

    public Instruction(String name, LLVMType type, InstrType instrType, BasicBlock parentBlock) {
        super(name, type);
        this.instrType = instrType;
        this.parentBlock = parentBlock;
    }

    public InstrType getInstrType() {
        return instrType;
    }

    public BasicBlock getParentBlock() {
        return parentBlock;
    }
}

