package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Constant;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Value;

/**
 * 表示带偏移量的加载指令 (OffsetLoad)
 */
public class OffsetLoad extends Instruction {

    /**
     * 构造 OffsetLoad 指令
     *
     * @param name   指令名称（加载结果的标识符）
     * @param pointer 指针操作数
     * @param offset 偏移量常量
     * @param parent 当前指令所属基本块
     */
    public OffsetLoad(String name, Value pointer, Constant offset, BasicBlock parent) {
        super(name, LLVMType.Int32, InstrType.LOAD, parent);

        if (pointer == null || offset == null || parent == null) {
            throw new IllegalArgumentException("Pointer, offset, and parent block cannot be null");
        }

        if (!(pointer.getType() instanceof PointerType)) {
            throw new IllegalArgumentException(
                    "OffsetLoad expects a pointer type for the first operand, but got: " + pointer.getType());
        }

        addOperand(pointer);
        addOperand(offset);
    }

    /**
     * 返回 LLVM IR 格式的偏移加载指令字符串
     *
     * @return 格式化的指令字符串
     */
    @Override
    public String toString() {
        Value pointer = operands.get(0);
        Constant offset = (Constant) operands.get(1);

        return String.format("%s = offsetLoad %s, %s %s(%d)",
                Name,
                type,
                pointer.getType(),
                pointer.getName(),
                offset.getValue());
    }
}
