package ast;

/**
 * 表达式节点
 * 对应文法：Exp → AddExp
 */
public class ExpNode {
    private AddExpNode addExpNode;

    public ExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }
    public int evaluate(){
        return addExpNode.evaluate();
    }
    public void print() {
        addExpNode.print();
        System.out.println("<Exp>");
    }
}
