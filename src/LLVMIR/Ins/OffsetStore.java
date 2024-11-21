package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Constant;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Value;

/**
 * 表示带偏移量的存储指令 (OffsetStore)
 */
public class OffsetStore extends Instruction {

    /**
     * 构造 OffsetStore 指令
     *
     * @param name   指令名称
     * @param pointer 指针操作数（存储目标）
     * @param offset 偏移量常量
     * @param parent 当前指令所属基本块
     * @param value  存储的值
     */
    public OffsetStore(String name, Value pointer, Constant offset, BasicBlock parent, Value value) {
        super(name, LLVMType.Void, InstrType.STORE, parent);

        if (pointer == null || offset == null || value == null || parent == null) {
            throw new IllegalArgumentException("Pointer, offset, value, and parent block cannot be null");
        }

        if (!(pointer.getType() instanceof PointerType)) {
            throw new IllegalArgumentException(
                    "OffsetStore expects a pointer type for the pointer operand, but got: " + pointer.getType());
        }

        addOperand(value);
        addOperand(pointer);
        addOperand(offset);
    }

    /**
     * 返回 LLVM IR 格式的偏移存储指令字符串
     *
     * @return 格式化的指令字符串
     */
    @Override
    public String toString() {
        Value value = operands.get(0);
        Value pointer = operands.get(1);
        Constant offset = (Constant) operands.get(2);

        return String.format("offsetStore %s %s, %s (%d)%s",
                value.getType(),
                value.getName(),
                pointer.getType(),
                offset.getValue(),
                pointer.getName());
    }
}
