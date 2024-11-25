package backEnd.Base;

/**
 * Register 枚举表示 MIPS 寄存器。
 */
public enum Register {
    // 寄存器名称和用途
    ZERO("$zero"), // 常量0
    AT("$at"), // 保留给汇编器使用的临时变量
    V0("$v0"), // 函数调用返回值
    V1("$v1"), // 函数调用返回值
    A0("$a0"), // 函数调用参数
    A1("$a1"), // 函数调用参数
    A2("$a2"), // 函数调用参数
    A3("$a3"), // 函数调用参数
    T0("$t0"), // 临时变量
    T1("$t1"), // 临时变量
    T2("$t2"), // 临时变量
    T3("$t3"), // 临时变量
    T4("$t4"), // 临时变量
    T5("$t5"), // 临时变量
    T6("$t6"), // 临时变量
    T7("$t7"), // 临时变量
    S0("$s0"), // 需要保存的变量
    S1("$s1"), // 需要保存的变量
    S2("$s2"), // 需要保存的变量
    S3("$s3"), // 需要保存的变量
    S4("$s4"), // 需要保存的变量
    S5("$s5"), // 需要保存的变量
    S6("$s6"), // 需要保存的变量
    S7("$s7"), // 需要保存的变量
    T8("$t8"), // 临时变量
    T9("$t9"), // 临时变量
    K0("$k0"), // 留给操作系统使用
    K1("$k1"), // 留给操作系统使用
    GP("$gp"), // 全局指针
    SP("$sp"), // 堆栈指针
    FP("$fp"), // 帧指针
    RA("$ra"); // 返回地址

    private String name;

    /**
     * 构造一个带有指定名称的 Register 枚举实例。
     *
     * @param name 寄存器的名称
     */
    Register(String name) {
        this.name = name;
    }

    /**
     * 根据索引获取寄存器。
     *
     * @param index 寄存器的索引
     * @return 对应的寄存器
     */
    public static Register getRegister(int index) {
        return Register.values()[index];
    }

    /**
     * 返回寄存器的名称。
     *
     * @return 寄存器的名称
     */
    @Override
    public String toString() {
        return name;
    }
}