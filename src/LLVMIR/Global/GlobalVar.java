package LLVMIR.Global;

import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Value;

import java.util.ArrayList;

public class GlobalVar extends Value {
    private ArrayList<Integer> initial;
    private boolean isZeroInitial;
    private int len;
    private boolean isConst;
    public GlobalVar(String name, LLVMType type, ArrayList<Integer> initial, boolean isZeroInitial, int len, boolean isConst) {
        super(name, type); //type为对Symbol的类型的point类型，symbol类型为array或int32
        this.initial = initial;
        this.isZeroInitial = isZeroInitial;
        this.len = len;
        if(isZeroInitial){
            for(int i=1;i<=len;i++){
                this.initial.add(0);
            }
        }
        this.isConst=isConst;
    }
    @Override
    public String toString() {
        StringBuilder initStr;

        // 获取指向的类型
        LLVMType pointedType = ((PointerType) type).getPointedType();

        if (pointedType.isArray()) {
            // 如果是数组类型
            initStr = new StringBuilder("[" + len + " x ");
            initStr.append(pointedType.isInt8() ? "i8" : "i32").append("] ");

            if (isZeroInitial) {
                initStr.append("zeroinitializer");
            } else {
                initStr.append("[");
                for (int i = 0; i < initial.size(); i++) {
                    if (i > 0) {
                        initStr.append(", ");
                    }
                    initStr.append(pointedType.isInt8() ? "i8 " : "i32 ")
                            .append(pointedType.isInt8() ? (initial.get(i) & 0xFF) : initial.get(i));
                }
                initStr.append("]");
            }
        } else {
            // 非数组类型
            if (isZeroInitial) {
                initStr = new StringBuilder(pointedType.isInt8() ? "i8 0" : "i32 0");
            } else {
                int value = initial.get(0);
                if (pointedType.isInt8()) {
                    value &= 0xFF; // 截断为 i8
                }
                initStr = new StringBuilder(pointedType.isInt8() ? "i8 " : "i32 ")
                        .append(value);
            }
        }

        return Name + " = dso_local global " + initStr;
    }

}
