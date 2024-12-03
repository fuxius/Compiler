package backEnd.Global;

public class AsciiAsm extends GlobalAsm {
    private String ascii;

    public AsciiAsm(String label, String ascii) {
        super(label);
        this.ascii = ascii;
    }

    @Override
    public String toString() {
        // 对字符串中的特殊字符进行转义
        String escapedAscii = escapeSpecialChars(ascii);
        return label + ": .asciiz \"" + escapedAscii + "\"";
    }

    private String escapeSpecialChars(String input) {
        // 转义表：替换常见转义字符
        return input.replace("\\", "\\\\") // 反斜杠
                .replace("\'", "\\\'") // 单引号
                .replace("\"", "\\\"") // 双引号
                .replace("\b", "\\b") // 退格
                .replace("\t", "\\t") // 水平制表符
                .replace("\n", "\\n") // 换行
                .replace("\f", "\\f") // 换页
                .replace("\r", "\\r") // 回车
                .replace("\0", "\\0") ;// 空字符
    }
}
