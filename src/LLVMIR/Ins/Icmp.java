package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

/**
 * 表示 LLVM 中的整数比较指令
 */
public class Icmp extends Instruction {

    public enum OP {
        EQ,
        NE,
        SLT,
        SLE,
        SGT,
        SGE;

        /**
         * 获取对称操作符
         *
         * @return 对称的操作符
         */
        public OP getSymmetric() {
            switch (this) {
                case SGE: return SLE;
                case SLE: return SGE;
                case SGT: return SLT;
                case SLT: return SGT;
                default: return this; // EQ 和 NE 是对称的
            }
        }
    }

    private final OP op;

    /**
     * 构造整数比较指令
     *
     * @param v1     第一个操作数
     * @param v2     第二个操作数
     * @param name   指令名称
     * @param parent 所属基本块
     * @param op     比较操作符
     */
    public Icmp(Value v1, Value v2, String name, BasicBlock parent, OP op) {
        super(name, LLVMType.Int1, InstrType.ICMP, parent);

        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }

        addOperand(v1);
        addOperand(v2);
        this.op = op;
    }


    /**
     * 获取比较的操作数类型
     *
     * @return 比较操作数的类型
     */
    public LLVMType cmpType() {
        return operands.get(0).getType();
    }

    /**
     * 返回 LLVM IR 格式的整数比较指令字符串
     *
     * @return 格式化的指令字符串
     */
    @Override
    public String toString() {
        String type = cmpType().equals(LLVMType.Int32) ? "i32" : "i1";
        return String.format("%s = icmp %s %s %s, %s",
                Name,
                op.toString().toLowerCase(),
                type,
                operands.get(0).getName(),
                operands.get(1).getName());
    }


    /**
     * 获取比较操作符
     *
     * @return 比较操作符
     */
    public OP getOp() {
        return op;
    }

    //遍历User检查该比较指令是否只是被用作控制流的条件判断（Branch），或者它是否有其他的计算或赋值用途。
    public boolean isControlFlow() {
        for (Value user : this.getUsers()) {
            if (!(user instanceof Branch)) {
                return false;
            }
        }
        return true;
    }

}
