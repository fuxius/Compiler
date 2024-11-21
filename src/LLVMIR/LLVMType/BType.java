package LLVMIR.LLVMType;

public class BType extends LLVMType {
    private int typeName; // 类型名称，例如 "i32"、"i8"、"i1"

    public BType(int typeName) {
        this.typeName = typeName;
    }

    public int getTypeName() {
        return typeName;
    }
    public String toString(){
        if(typeName==0){
            return "void";
        }
        else if(typeName==32){
            return "i32";
        }
        else if(typeName==8){
            return "i8";
        }
        else if(typeName==1){
            return "i1";
        }
        return null;
    }
}
