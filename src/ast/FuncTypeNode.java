package ast;

/**
 * 函数类型节点
 * 对应文法：FuncType → 'void' | 'int' | 'char'
 */
public class FuncTypeNode {
    private String type; // 'void'，'int'，或 'char'

    public FuncTypeNode(String type) {
        this.type = type;
    }

    public void print() {
        if (type.equals("void")) {
            System.out.println("VOIDTK void");
        } else if (type.equals("int")) {
            System.out.println("INTTK int");
        } else if (type.equals("char")) {
            System.out.println("CHARTK char");
        }
        // 不需要输出 <FuncType>，根据您的要求
        System.out.println("<FuncType>");
    }
}
