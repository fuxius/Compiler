package ast;

import java.util.List;

/**
 * 一元表达式节点
 * 对应文法：
 * UnaryExp → PrimaryExp
 *            | Ident '(' [FuncRParams] ')'
 *            | UnaryOp UnaryExp
 */
public class UnaryExpNode {
    private PrimaryExpNode primaryExpNode;
    private String ident;
    private FuncRParamsNode funcRParamsNode; // 可选
    private UnaryOpNode unaryOpNode;
    private UnaryExpNode unaryExpNode;

    // PrimaryExp
    public UnaryExpNode(PrimaryExpNode primaryExpNode) {
        this.primaryExpNode = primaryExpNode;
    }

    // Ident '(' [FuncRParams] ')'
    public UnaryExpNode(String ident, FuncRParamsNode funcRParamsNode) {
        this.ident = ident;
        this.funcRParamsNode = funcRParamsNode;
    }

    // UnaryOp UnaryExp
    public UnaryExpNode(UnaryOpNode unaryOpNode, UnaryExpNode unaryExpNode) {
        this.unaryOpNode = unaryOpNode;
        this.unaryExpNode = unaryExpNode;
    }

    public void print() {
        if (primaryExpNode != null) {
            primaryExpNode.print();
        } else if (ident != null) {
            System.out.println("IDENFR " + ident);
            System.out.println("LPARENT (");
            if (funcRParamsNode != null) {
                funcRParamsNode.print();
            }
            System.out.println("RPARENT )");
        } else if (unaryOpNode != null && unaryExpNode != null) {
            unaryOpNode.print();
            unaryExpNode.print();
        }
        System.out.println("<UnaryExp>");
    }
}
