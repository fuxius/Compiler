package ast;

import java.util.List;

/**
 * 左值表达式节点
 * 对应文法：LVal → Ident ['[' Exp ']']
 */
public class LValNode {
    private String ident;
    private ExpNode expNode; // 可选的数组下标

    public LValNode(String ident, ExpNode expNode) {
        this.ident = ident;
        this.expNode = expNode;
    }

    public void print() {
        System.out.println("IDENFR " + ident);
        if (expNode != null) {
            System.out.println("LBRACK [");
            expNode.print();
            System.out.println("RBRACK ]");
        }
        System.out.println("<LVal>");
    }
}
