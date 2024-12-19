package LLVMIR.Global;

import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.LLVMType.ArrayType;
import LLVMIR.Base.Core.Value;
import java.util.Objects;

/**
 * ConstStr: 表示LLVM IR中的字符串常量
 */
public class ConstStr extends Value {
    private final String value; // 字符串内容

    /**
     * 构造字符串常量
     *
     * @param name  字符串变量名
     * @param value 字符串值
     */
    public ConstStr(String name, String value) {
        super(name, createPointerType(value)); // 基于字符串值动态创建类型
        this.value = Objects.requireNonNull(value, "String value cannot be null");
    }

    /**
     * 获取字符串值
     *
     * @return 字符串内容
     */
    public String getValue() {
        return value;
    }

    /**
     * 返回LLVM IR格式字符串
     *
     * @return 格式化的LLVM字符串常量定义
     */
    @Override
    public String toString() {
        LLVMType pointedType = ((PointerType) type).getPointedType();

        // 确保类型安全
        if (!(pointedType instanceof ArrayType)) {
            throw new IllegalStateException("Unexpected pointed type for ConstStr: " + pointedType);
        }

        return String.format("%s = private unnamed_addr constant %s c\"%s\\00\"",
                Name, pointedType, escapeValue(value));
    }

    /**
     * 创建指向字符串的指针类型
     *
     * @param value 字符串值
     * @return 指针类型
     */
    private static PointerType createPointerType(String value) {
        int lengthWithNull = value.length() + 1; // 字符串末尾需包含'\0'
        ArrayType arrayType = new ArrayType(LLVMType.Int8, lengthWithNull);
        return new PointerType(arrayType);
    }

    /**
     * 转义字符串值，确保符合LLVM的转义规则
     *
     * @param value 原始字符串值
     * @return 转义后的字符串
     */
    private static String escapeValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
