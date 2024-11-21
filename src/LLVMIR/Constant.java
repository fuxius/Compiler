package LLVMIR;

import LLVMIR.LLVMType.LLVMType;
//Constant 类表示LLVM中的常量，可能是整数、浮点数等常量。常量是不可变的值
public class Constant extends Value {
    private int value;
    public Constant(int value){
        super(String.valueOf(value), LLVMType.Int32);
        this.value=value;
    }

    public int getValue() {
        return value;
    }
}
