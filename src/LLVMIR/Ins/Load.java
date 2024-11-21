package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Value;

public class Load extends Instruction {
    public Load(String name, Value point, BasicBlock parent) {
        super(name, getTypeFromPointer(point), InstrType.LOAD, parent);
        addOperand(point);
    }

    // 根据指针类型获取加载结果的类型
    private static LLVMType getTypeFromPointer(Value point) {
        if (point.getType() instanceof PointerType) {
            return ((PointerType) point.getType()).getPointedType();
        }
        throw new IllegalArgumentException("Load expects a pointer type, but got: " + point.getType());
    }

    public String toString() {
        return Name + " = load " + type + ", " + operands.get(0).getType() + " " + operands.get(0).getName();
    }
}
