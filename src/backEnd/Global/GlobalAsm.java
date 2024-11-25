package backEnd.Global;

import backEnd.Base.AsmInstruction;

/**
 * GlobalAsm 类表示一个全局汇编指令。
 * 它继承自 AsmInstruction 类。
 */
public class GlobalAsm extends AsmInstruction {
    private String name;

    /**
     * 构造一个带有指定名称的 GlobalAsm 对象。
     *
     * @param name 汇编指令的名称
     */
    public GlobalAsm(String name) {
        this.name = name;
    }

    /**
     * 返回 GlobalAsm 对象的字符串表示形式。
     *
     * @return GlobalAsm 对象的字符串表示形式
     */
    public String toString() {
        return name + ":";
    }
}