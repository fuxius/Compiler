package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Value;

/**
 * 表示 LLVM 中的 GetPtr 指令
 */
public class GetPtr extends Instruction {

    /**
     * 构造 GetPtr 指令
     *
     * @param name   指令名称（结果指针的标识符）
     * @param array  基础数组指针
     * @param offset 偏移量
     * @param parent 所属基本块
     */
    public GetPtr(String name, Value array, Value offset, BasicBlock parent) {
        super(name, new PointerType(LLVMType.Int32), InstrType.GETPTR, parent);

        if (array == null || offset == null || parent == null) {
            throw new IllegalArgumentException("Array, offset, and parent block cannot be null");
        }
        if (!(array.getType() instanceof PointerType)) {
            throw new IllegalArgumentException("Array must be of PointerType");
        }

        addOperand(array);
        addOperand(offset);
    }

    /**
     * 返回 LLVM IR 格式的 GetPtr 指令字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        Value pointer = operands.get(0); // 数组指针
        Value offset = operands.get(1); // 偏移量

        if (!(pointer.getType() instanceof PointerType)) {
            throw new IllegalStateException("Pointer operand must be of PointerType");
        }

        PointerType pointerType = (PointerType) pointer.getType();
        LLVMType pointedType = pointerType.getPointedType();

        if (pointedType.isInt32()) {
            // 简化形式：处理基本类型的偏移
            return String.format("%s = getelementptr i32, i32* %s, i32 %s",
                    Name, pointer.getName(), offset.getName());
        } else {
            // 完整形式：处理数组的偏移
            return String.format("%s = getelementptr %s, %s %s, i32 0, i32 %s",
                    Name, pointedType, pointerType, pointer.getName(), offset.getName());
        }
    }
}
