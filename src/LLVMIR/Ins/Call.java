package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.Base.Value;
import LLVMIR.Global.Function;

import java.util.List;

/**
 * 表示函数调用指令
 */
public class Call extends Instruction {

    /**
     * 构造函数调用指令
     *
     * @param func    调用的函数
     * @param name    指令名（返回值的标识符）
     * @param values  参数列表
     * @param parent  所属基本块
     */
    public Call(Function func, String name, List<Value> values, BasicBlock parent) {
        super(name, func.getReturnType(), InstrType.CALL, parent);

        if (func == null || values == null) {
            throw new IllegalArgumentException("Function and values cannot be null");
        }

        addOperand(func); // 函数作为第一个操作数
        for (Value value : values) {
            if (value == null) {
                throw new IllegalArgumentException("Operands in values cannot be null");
            }
            addOperand(value);
        }
    }

    /**
     * 返回 LLVM IR 格式的函数调用指令字符串
     *
     * @return 格式化的函数调用指令字符串
     */
    @Override
    public String toString() {
        StringBuilder args = new StringBuilder();

        for (int i = 1; i < operands.size(); i++) {
            if (i > 1) {
                args.append(", ");
            }
            args.append(operands.get(i).getType()).append(" ").append(operands.get(i).getName());
        }

        String callType = type.isVoid() ? "void" : type.toString();
        String callPrefix = type.isVoid() ? "call" : (Name + " = call");

        return String.format("%s %s %s(%s)", callPrefix, callType, operands.get(0).getName(), args.toString());
    }
}
