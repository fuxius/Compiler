package ast;

/**
 * 常量表达式节点
 * 对应文法：ConstExp → AddExp
 */
public class ConstExpNode {
    private AddExpNode addExpNode;
    int result;

    public ConstExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }
    public int evaluate(){
        result = addExpNode.evaluate();
        return addExpNode.evaluate();
    }
    public void print() {
        addExpNode.print();
        // 不需要输出 <ConstExp>，根据您的要求
    }
}
