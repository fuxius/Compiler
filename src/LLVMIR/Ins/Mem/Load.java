package LLVMIR.Ins.Mem;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Core.Value;

/**
 * 表示 LLVM IR 中的加载指令 (load)
 */
public class Load extends Instruction {

    /**
     * 构造加载指令
     *
     * @param name   指令名称（加载结果的标识符）
     * @param pointer 指针操作数
     * @param parent 当前指令所属基本块
     */
    public Load(String name, Value pointer, BasicBlock parent) {
        super(name, getTypeFromPointer(pointer), InstrType.LOAD, parent);

        if (pointer == null) {
            throw new IllegalArgumentException("Pointer operand cannot be null");
        }

        addOperand(pointer);
    }

    /**
     * 根据指针操作数获取加载结果的类型
     *
     * @param pointer 指针操作数
     * @return 加载结果的类型
     */
    private static LLVMType getTypeFromPointer(Value pointer) {
        if (!(pointer.getType() instanceof PointerType)) {
            throw new IllegalArgumentException(
                    "Load instruction expects a pointer type, but got: " + pointer.getType());
        }
        return ((PointerType) pointer.getType()).getPointedType();
    }

    /**
     * 返回 LLVM IR 格式的加载指令字符串
     *
     * @return 格式化的指令字符串
     */
    @Override
    public String toString() {
        return String.format("%s = load %s, %s %s",
                Name,
                type,
                operands.get(0).getType(),
                operands.get(0).getName());
    }
}
