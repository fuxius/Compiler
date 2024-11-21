package LLVMIR;

import LLVMIR.Global.Function;
import LLVMIR.LLVMType.LLVMType;

public class Param extends Value {
    private Function parentFunction;
    public Param(String name, LLVMType type,Function parentFunction) {
        super(name, type);
        this.parentFunction = parentFunction;
    }
    public String toString(){
        return type+" "+ Name;
    }
}
