package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Global.ConstStr;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Value;

public class Putstr extends Instruction {
    private ConstStr constStr;

    public Putstr(BasicBlock parentBlock,ConstStr constStr) {
        super(null, LLVMType.Void, InstrType.PUTSTR, parentBlock);
        this.constStr = constStr;
    }
    public String toString() {
        PointerType pointerType = (PointerType) constStr.getType();
        return "call void @putstr(i8* getelementptr inbounds (" +
                pointerType.getPointedType() + ", " +
                pointerType + " " +
                constStr.getName() +", i64 0, i64 0))";
    }
}
