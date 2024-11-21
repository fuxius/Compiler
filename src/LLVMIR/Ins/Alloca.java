package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;

import java.util.ArrayList;

public class Alloca extends Instruction {
    private LLVMType pointedType;
    private boolean isConst = false;

    private ArrayList<Integer> initials;

    public Alloca(String name,  BasicBlock parentBlock, LLVMType pointedType) {
        super(name, new PointerType(pointedType), InstrType.ALLOCA, parentBlock);
        this.pointedType = pointedType;
    }
    public Alloca(String name,  BasicBlock parentBlock, LLVMType pointedType,ArrayList<Integer> initial) {
        super(name, new PointerType(pointedType), InstrType.ALLOCA, parentBlock);
        this.pointedType = pointedType;
        this.isConst=true;
        this.initials=initial;
    }
    public boolean isConst() {
        return isConst;
    }

    public ArrayList<Integer> getInitial() {
        return initials;
    }

    public LLVMType getPointedType() {
        return pointedType;
    }

    public String toString() {
        return Name + " = alloca " + pointedType;
    }
}
