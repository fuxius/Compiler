package ast;

/**
 * 基本类型节点
 * 对应文法：BType → 'int' | 'char'
 */
public class BTypeNode {
    private String type; // 'int' 或 'char'

    public BTypeNode(String type) {
        this.type = type;
    }

    public void print() {
        if (type.equals("int")) {
            System.out.println("INTTK int");
        } else if (type.equals("char")) {
            System.out.println("CHARTK char");
        }

        // 不需要输出 <BType>
    }
}
