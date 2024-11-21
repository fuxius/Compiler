package LLVMIR;

import LLVMIR.LLVMType.LLVMType;

public class Undef extends Constant {
    public Undef() {
        super(0);
    }
    public String toString(){
        return "undef";
    }
}
