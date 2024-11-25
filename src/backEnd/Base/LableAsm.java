package backEnd.Base;

public class LableAsm extends AsmInstruction{
    //标签的名称、是否属于一个代码块
    private String name;
    private boolean isBlock;

    public LableAsm(String name, boolean isBlock) {
        this.name = name;
        this.isBlock = isBlock;
    }

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return isBlock;
    }

    @Override
    public String toString() {
        //分情况
        if (isBlock) {
            return name + ":";
        } else {
            return name;
        }
    }
}
