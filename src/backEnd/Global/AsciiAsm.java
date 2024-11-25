package backEnd.Global;

/**
 * AsciiAsm 类表示包含 ASCII 字符串的汇编指令。
 * 它继承自 GlobalAsm 类。
 */
public class AsciiAsm extends GlobalAsm {
    private String ascii;

    /**
     * 构造一个带有指定名称和 ASCII 字符串的 AsciiAsm 对象。
     *
     * @param name  汇编指令的名称
     * @param ascii 与指令关联的 ASCII 字符串
     */
    public AsciiAsm(String name, String ascii) {
        super(name);
        this.ascii = ascii;
    }

    /**
     * 返回 AsciiAsm 对象的字符串表示形式。
     * ASCII 字符串被转义以确保特殊字符被正确表示。
     *
     * @return AsciiAsm 对象的字符串表示形式
     */
    @Override
    public String toString() {
        // 转义 ASCII 字符串中的反斜杠
        ascii = ascii.replace("\\", "\\\\");
        return super.toString() + " .ascii \"" + ascii + "\"";
    }
}

