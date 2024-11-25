package backEnd.Global;

/**
 * SpaceAsm 类表示一个包含空间大小的汇编指令。
 * 它继承自 GlobalAsm 类。
 */
public class SpaceAsm extends GlobalAsm {
    private int size;

    /**
     * 构造一个带有指定名称和空间大小的 SpaceAsm 对象。
     *
     * @param name 汇编指令的名称
     * @param size 空间的大小
     */
    public SpaceAsm(String name, int size) {
        super(name);
        this.size = size;
    }

    /**
     * 返回 SpaceAsm 对象的字符串表示形式。
     *
     * @return SpaceAsm 对象的字符串表示形式
     */
    public String toString() {
        return super.toString() + " .space " + size;
    }
}


