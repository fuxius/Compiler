package ast;

/**
 * 字符节点
 * 对应文法：Character → CharConst
 */
public class CharacterNode {
    private String charConst;

    public CharacterNode(String charConst) {
        this.charConst = charConst;
    }

    public void print() {
        System.out.println("CHARCON " + charConst);
        // 不需要输出 <Character>，根据您的要求
        System.out.println("<Character>");
    }
}
