package LLVMIR.Ins.Mem;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;

import java.util.ArrayList;

/**
 * 表示 LLVM 中的内存分配指令
 */
public class Alloca extends Instruction {
    private final LLVMType pointedType;  // 指针指向的类型
    private final boolean isConst;       // 是否为常量
    private final ArrayList<Integer> initials; // 初始值（仅适用于数组）

    /**
     * 构造标量 Alloca 指令
     *
     * @param name        指令名
     * @param parentBlock 所属基本块
     * @param pointedType 指针指向的类型
     */
    public Alloca(String name, BasicBlock parentBlock, LLVMType pointedType) {
        super(name, new PointerType(pointedType), InstrType.ALLOCA, parentBlock);
        this.pointedType = pointedType;
        this.isConst = false;
        this.initials = null; // 标量没有初始值
    }

    /**
     * 构造数组 Alloca 指令
     *
     * @param name        指令名
     * @param parentBlock 所属基本块
     * @param pointedType 指针指向的类型
     * @param initial     数组的初始值
     */
    public Alloca(String name, BasicBlock parentBlock, LLVMType pointedType, ArrayList<Integer> initial) {
        super(name, new PointerType(pointedType), InstrType.ALLOCA, parentBlock);
        this.pointedType = pointedType;
        this.isConst = true;
        this.initials = initial != null ? new ArrayList<>(initial) : new ArrayList<>(); // 深拷贝初始值
    }

    public boolean isConst() {
        return isConst;
    }

    public ArrayList<Integer> getInitial() {
        return initials != null ? new ArrayList<>(initials) : new ArrayList<>(); // 返回副本
    }

    public LLVMType getPointedType() {
        return pointedType;
    }

    @Override
    public String toString() {
        return Name + " = alloca " + pointedType;
    }
}
