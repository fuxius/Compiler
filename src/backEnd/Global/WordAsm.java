package backEnd.Global;

import java.util.ArrayList;

/**
 * WordAsm 类表示一个包含多个整数的汇编指令。
 * 它继承自 GlobalAsm 类。
 */
public class WordAsm extends GlobalAsm {
    private ArrayList<Integer> words;

    /**
     * 构造一个带有指定名称和整数列表的 WordAsm 对象。
     *
     * @param name  汇编指令的名称
     * @param words 整数列表
     */
    public WordAsm(String name, ArrayList<Integer> words) {
        super(name);
        this.words = words;
    }

    /**
     * 返回 WordAsm 对象的字符串表示形式。
     *
     * @return WordAsm 对象的字符串表示形式
     */
    public String toString() {
        String result = super.toString() + " .word ";
        for (int i = 0; i < words.size(); i++) {
            result += words.get(i);
            if (i != words.size() - 1) {
                result += ", ";
            }
        }
        return result;
    }
}