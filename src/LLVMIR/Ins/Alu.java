package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

/**
 * 表示 ALU 运算指令 (如加法、减法等)
 */
public class Alu extends Instruction {

    /**
     * 表示支持的操作类型
     */
    public enum OP {
        ADD,
        SUB,
        SREM,
        MUL,
        SDIV;

        /**
         * 判断操作是否需要 nsw 标志
         *
         * @return true 如果操作支持 nsw 标志
         */
        public boolean supportsNSW() {
            return this == ADD || this == SUB || this == MUL;
        }
    }

    private OP op; // ALU 操作类型

    /**
     * 构造 ALU 指令
     *
     * @param name      指令名
     * @param v1        第一个操作数
     * @param v2        第二个操作数
     * @param op        操作类型
     * @param parent    所属基本块
     */
    public Alu(String name, Value v1, Value v2, OP op, BasicBlock parent) {
        super(name, LLVMType.Int32, InstrType.ALU, parent);

        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        if (op == null) {
            throw new IllegalArgumentException("Operation type cannot be null");
        }

        this.op = op;
        addOperand(v1);
        addOperand(v2);
    }

    /**
     * 获取 ALU 操作类型
     *
     * @return 操作类型
     */
    public OP getOp() {
        return op;
    }

    /**
     * 返回 LLVM IR 格式的指令字符串
     *
     * @return 格式化的指令字符串
     */
    @Override
    public String toString() {
        // 判断是否需要 nsw 标志
        String nswFlag = op.supportsNSW() ? " nsw" : "";

        // 构造指令字符串
        return String.format("%s = %s%s i32 %s, %s",
                Name,
                op.toString().toLowerCase(),
                nswFlag,
                operands.get(0).getName(),
                operands.get(1).getName()
        );
    }
    public void setOp(OP op) {
        this.op = op;
    }
    public String getGvnHash(){
        String op1=operands.get(0).getName();
        String op2=operands.get(1).getName();
        if(op==OP.ADD || op==OP.MUL){
            if(op1.compareTo(op2)<0){
                op2=operands.get(0).getName();
                op1=operands.get(1).getName();
            }
        }
        return op1+op+op2;
    }
}
