package LLVMIR.Global;

import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Value;

import java.util.ArrayList;
import java.util.Objects;

/**
 * 表示LLVM中的全局变量
 */
public class GlobalVar extends Value {
    private final ArrayList<Integer> initial; // 初始值列表
    private final boolean isZeroInitial;     // 是否零初始化
    private final int len;                   // 数组长度
    private final boolean isConst;           // 是否为常量

    /**
     * 构造全局变量（非字符串类型）
     *
     * @param name          全局变量名
     * @param type          全局变量类型
     * @param initial       初始值
     * @param isZeroInitial 是否零初始化
     * @param len           数组长度
     * @param isConst       是否为常量
     */
    public GlobalVar(String name, LLVMType type, ArrayList<Integer> initial, boolean isZeroInitial, int len, boolean isConst) {
        super(name, type);

        this.len = len;
        this.isConst = isConst;
        this.isZeroInitial = isZeroInitial;

        // 初始化列表
        this.initial = Objects.requireNonNullElseGet(initial, ArrayList::new);

        // 如果是零初始化，填充默认值
        if (isZeroInitial) {
            initializeWithZero();
        }else {
            // 如果初始值列表长度小于数组长度，填充零值
            while (this.initial.size() < len) {
                this.initial.add(0);
            }
        }
    }

    /**
     * 填充零值到初始化列表
     */
    private void initializeWithZero() {
        for (int i = 0; i < len; i++) {
            this.initial.add(0);
        }
    }

    /**
     * 返回LLVM IR格式字符串
     *
     * @return 格式化的全局变量定义
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(Name).append(" = dso_local global ");
        LLVMType pointedType = getPointedType();

        if (pointedType.isArray()) {
            // 处理数组类型
            result.append(getArrayRepresentation(pointedType));
        } else {
            // 处理非数组类型
            result.append(getNonArrayRepresentation(pointedType));
        }

        return result.toString();
    }

    /**
     * 获取指向的类型
     *
     * @return 指向的LLVM类型
     */
    private LLVMType getPointedType() {
        if (!(type instanceof PointerType)) {
            throw new IllegalStateException("GlobalVar type must be a PointerType");
        }
        return ((PointerType) type).getPointedType();
    }


    /**
     * 构造数组类型的字符串表示
     *
     * @param pointedType 指向的类型
     * @return 数组类型的字符串
     */
    private String getArrayRepresentation(LLVMType pointedType) {
        ArrayType Type = (ArrayType) pointedType;
        StringBuilder arrayBuilder = new StringBuilder("[")
                .append(len)
                .append(" x ")
                .append(Type.getElementType().isInt8() ? "i8" : "i32")
                .append("] ");

        if (isZeroInitial) {
            arrayBuilder.append("zeroinitializer");
        } else {
            arrayBuilder.append("[");
            for (int i = 0; i < initial.size(); i++) {
                if (i > 0) {
                    arrayBuilder.append(", ");
                }
                arrayBuilder.append(Type.getElementType().isInt8() ? "i8 " : "i32 ")
                        .append(Type.getElementType().isInt8() ? (initial.get(i) & 0xFF) : initial.get(i));
            }
            arrayBuilder.append("]");
        }

        return arrayBuilder.toString();
    }

    /**
     * 构造非数组类型的字符串表示
     *
     * @param pointedType 指向的类型
     * @return 非数组类型的字符串
     */
    private String getNonArrayRepresentation(LLVMType pointedType) {
        if (isZeroInitial) {
            return pointedType.isInt8() ? "i8 0" : "i32 0";
        } else {
            int value = initial.isEmpty() ? 0 : initial.get(0); // 如果初始值为空，使用默认值0
            return (pointedType.isInt8() ? "i8 " : "i32 ") + (pointedType.isInt8() ? (value & 0xFF) : value);
        }
    }

    // 初始值列表
    public ArrayList<Integer> getInitial() {
        return initial;
    }

    // 是否零初始化
    public boolean isZeroInitial() {
        return isZeroInitial;
    }

    // 是否为常量
    public boolean isConst() {
        return isConst;
    }
    // 数组长度
    public int getLen() {
        return len;
    }

}
