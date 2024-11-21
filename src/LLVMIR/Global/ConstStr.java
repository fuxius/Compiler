package LLVMIR.Global;

import LLVMIR.LLVMType.BType;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.LLVMType.ArrayType;
import LLVMIR.Value;

public class ConstStr extends Value {
    private String value; // 字符串内容

    public ConstStr(String name, String value) {
        super(name, new PointerType(new ArrayType(LLVMType.Int8,value.length()+1))); // 字符串常量在LLVM中通常是i8数组类型
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return Name + " = private unnamed_addr constant " + ((PointerType)type).getPointedType()+ " c\"" + value + "\\00\"";
    }
}

