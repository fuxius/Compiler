package error;

public enum ErrorType {
    ILLEGAL_SYMBOL("a"), // 非法符号，例如 '&' 和 '|' 未组成 '&&' 和 '||'
    REDEFINED_IDENT("b"), // 名字重定义
    UNDEFINED_IDENT("c"), // 未定义的名字
    FUNC_PARAM_COUNT_MISMATCH("d"), // 函数参数个数不匹配
    FUNC_PARAM_TYPE_MISMATCH("e"), // 函数参数类型不匹配
    FUNC_RETURN_MISMATCH("f"), // 无返回值函数存在不匹配的return语句
    MISSING_RETURN_VALUE("g"), // 有返回值的函数缺少return语句
    MODIFY_CONST("h"), // 不能改变常量的值
    MISSING_SEMICOLON("i"), // 缺少分号
    MISSING_RIGHT_BRACKET("j"), // 缺少右小括号
    MISSING_RIGHT_RBRACK("k"), // 缺少右中括号
    PRINTF_ARG_MISMATCH("l"), // printf中格式字符与表达式个数不匹配
    BREAK_CONTINUE_OUTSIDE_LOOP("m"); // 在非循环中使用break和continue语句

    private String code;

    ErrorType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
