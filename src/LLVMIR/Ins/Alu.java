package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Value;

public class Alu extends Instruction {
    public enum OP{
        ADD,
        SUB,
        SREM,
        MUL,
        SDIV,
    }
    private OP op;
    public Alu(String name, Value v1, Value v2, OP op, BasicBlock parent){
        super(name, LLVMType.Int32,InstrType.ALU,parent);
        this.op=op;
        addOperand(v1);
        addOperand(v2);
    }
    @Override
    public String toString() {
        // 适用的运算类型使用 nsw 标志
        String nswFlag = (op == OP.ADD || op == OP.SUB || op == OP.MUL) ? " nsw" : "";
        return Name + " = " +  op.toString().toLowerCase() + nswFlag+ " i32 " + operands.get(0).getName() + ", " + operands.get(1).getName();
    }


    public OP getOp() {
        return op;
    }

    public void setOp(OP op) {
        this.op = op;
    }
}