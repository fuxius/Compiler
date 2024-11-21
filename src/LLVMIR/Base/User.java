package LLVMIR.Base;

import LLVMIR.Base.Value;
import LLVMIR.LLVMType.LLVMType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示 LLVM IR 中使用值的对象 (User)
 * 包含对操作数 (operands) 的引用。
 */
public class User extends Value {
    protected final ArrayList<Value> operands; // 操作数列表

    /**
     * 构造 User 对象
     *
     * @param name 名称
     * @param type 类型
     */
    public User(String name, LLVMType type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    /**
     * 添加操作数
     *
     * @param value 操作数
     */
    public void addOperand(Value value) {
        operands.add(value);
        if (value != null) {
            value.addUser(this);
        }
    }

    /**
     * 修改操作数
     *
     * @param oldValue 旧操作数
     * @param newValue 新操作数
     */
    public void modifyValue(Value oldValue, Value newValue) {
        if (oldValue == null || newValue == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        while (true) {
            int index = operands.indexOf(oldValue);
            if (index == -1) {
                break;
            }
            operands.set(index, newValue);
            newValue.addUser(this);
        }
    }

    /**
     * 移除所有操作数
     */
    public void removeOperands() {
        for (Value value : operands) {
            if (value != null) {
                value.removeUser(this);
            }
        }
        operands.clear();
    }

    /**
     * 设置指定位置的操作数
     *
     * @param value 操作数
     * @param pos   位置
     */
    public void setOperand(Value value, int pos) {
        if (pos < 0 || pos >= operands.size()) {
            throw new IndexOutOfBoundsException("Operand position out of bounds: " + pos);
        }
        operands.set(pos, value);
        if (value != null) {
            value.addUser(this);
        }
    }

    /**
     * 获取所有操作数
     *
     * @return 操作数列表的只读视图
     */
    public List<Value> getOperands() {
        return Collections.unmodifiableList(operands);
    }
}
