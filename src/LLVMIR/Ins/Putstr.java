package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Global.ConstStr;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;

public class Putstr extends Instruction {
    private final ConstStr constStr;

    public Putstr(BasicBlock parentBlock, ConstStr constStr) {
        super(null, LLVMType.Void, InstrType.PUTSTR, parentBlock);

        if (parentBlock == null || constStr == null) {
            throw new IllegalArgumentException("Parent block and constStr cannot be null");
        }

        this.constStr = constStr;
    }

    @Override
    public String toString() {
        PointerType pointerType = (PointerType) constStr.getType();
        return String.format("call void @putstr(i8* getelementptr inbounds (%s, %s %s, i64 0, i64 0))",
                pointerType.getPointedType(),
                pointerType,
                constStr.getName());
    }
}
