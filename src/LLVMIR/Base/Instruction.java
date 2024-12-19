package LLVMIR.Base;

import LLVMIR.LLVMType.LLVMType;

/**
 * 表示 LLVM IR 中的指令基类
 * 每个指令表示一个操作，具体指令类型由子类实现。
 */
public class Instruction extends User {

    /**
     * 指令类型枚举
     */
    public static enum InstrType {
        ALU, ALLOCA, BRANCH, CALL, GETPTR, ICMP, JUMP, LOAD, RETURN, STORE, ZEXT,
        GETINT, PUTSTR, PUTINT, PHI, MOVE, GETCHAR, PUTCH, TRUNC
    }

    private final InstrType instrType;     // 指令类型
    private BasicBlock parentBlock; // 指令所属的基本块

    /**
     * 构造指令对象
     *
     * @param name        指令名称
     * @param type        指令的返回类型
     * @param instrType   指令的类型枚举
     * @param parentBlock 指令所属的基本块
     */
    public Instruction(String name, LLVMType type, InstrType instrType, BasicBlock parentBlock) {
        super(name, type);

        if (instrType == null || parentBlock == null) {
            throw new IllegalArgumentException("Instruction type and parent block cannot be null");
        }

        this.instrType = instrType;
        this.parentBlock = parentBlock;
    }

    /**
     * 获取指令类型
     *
     * @return 指令类型
     */
    public InstrType getInstrType() {
        return instrType;
    }

    /**
     * 获取所属的基本块
     *
     * @return 基本块
     */
    public BasicBlock getParentBlock() {
        return parentBlock;
    }
    // setParentBlock()
    public void setParentBlock(BasicBlock parentBlock) {
        if (parentBlock == null) {
            throw new IllegalArgumentException("Parent block cannot be null");
        }
        this.parentBlock = parentBlock;
    }

    /**
     * 判断指令是否有LVal
     *
     * @return 是否有LVal
     */
    public boolean hasLVal() {
        //如果指令是Getint或者Getchar，那么它有LVal
        //如果指令是Alloca，那么它有LVal
        //如果指令是Load，那么它有LVal
        //如果指令是Getptr，那么它有LVal
        //如果指令是Zext，那么它有LVal
        //如果指令是Trunc，那么它有LVal
        //如果指令是Phi，那么它有LVal
        //如果指令是Alloca，那么它有LVal
        //如果指令是Call且类型不是Void，那么它有LVal
        //如果指令是Alu，那么它有LVal
        //如果指令是Icmp，那么它有LVal

        return instrType == InstrType.GETINT || instrType == InstrType.GETCHAR || instrType == InstrType.ALLOCA
                || instrType == InstrType.LOAD || instrType == InstrType.GETPTR || instrType == InstrType.ZEXT || instrType == InstrType.TRUNC || instrType == InstrType.PHI
                || (instrType == InstrType.CALL && !getType().isVoid()) || instrType == InstrType.ALU
                || instrType == InstrType.ICMP;
    }
}
