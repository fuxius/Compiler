package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Core.Value;

/**
 * 表示 LLVM 中的 GetElementPtr 指令
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
        super(name, null, InstrType.GETPTR, parent);

        if (array == null || offset == null || parent == null) {
            throw new IllegalArgumentException("Array, offset, and parent block cannot be null");
        }
        if (!(array.getType() instanceof PointerType)) {
            throw new IllegalArgumentException("Array must be of PointerType");
        }

        PointerType arrayPtrType = (PointerType) array.getType();
        LLVMType elementType;
        if (!(arrayPtrType.getPointedType() instanceof ArrayType)) {
            elementType = arrayPtrType.getPointedType();
        }else {
            ArrayType arrayType = (ArrayType) arrayPtrType.getPointedType();
            elementType = arrayType.getElementType();
        }

        // 设置指令的返回类型为指向元素类型的指针
        this.type = new PointerType(elementType);

        addOperand(array);
        addOperand(offset);
    }

    /**
     * 返回 LLVM IR 格式的 GetElementPtr 指令字符串
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
        LLVMType elementType = pointerType.getPointedType();

        String elementTypeStr = elementType.toString();
        String pointerTypeStr = pointerType.toString();

        // 判断是否为基本类型（i8 或 i32）
        if (elementType.isInt8() || elementType.isInt32()) {
            // 简化形式：处理基本类型的偏移
            return String.format("%s = getelementptr %s, %s %s, i32 %s",
                    Name, elementTypeStr, pointerTypeStr, pointer.getName(), offset.getName());
        } else {
            // 完整形式：处理复杂类型的偏移
            return String.format("%s = getelementptr %s, %s %s, i32 0, i32 %s",
                    Name, elementTypeStr, pointerTypeStr, pointer.getName(), offset.getName());
        }
    }
    public String getGvnHash(){
        return "GetPtr "+operands.get(0).getName()+" "+operands.get(1).getName();
    }
}
