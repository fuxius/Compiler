package ast;

/**
 * 关系表达式节点
 * 对应文法：RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
 */
public class RelExpNode {
    private RelExpNode relExpNode;
    private String op; // '<', '>', '<=', '>='
    private AddExpNode addExpNode;
    private AddExpNode singleAddExpNode;

    // 单个 AddExp
    public RelExpNode(AddExpNode singleAddExpNode) {
        this.singleAddExpNode = singleAddExpNode;
    }

    // RelExp ('<' | '>' | '<=' | '>=') AddExp
    public RelExpNode(RelExpNode relExpNode, String op, AddExpNode addExpNode) {
        this.relExpNode = relExpNode;
        this.op = op;
        this.addExpNode = addExpNode;
    }

    public void print() {
        if (singleAddExpNode != null) {
            singleAddExpNode.print();
        } else {
            relExpNode.print();
            switch (op) {
                case "<":
                    System.out.println("LSS <");
                    break;
                case ">":
                    System.out.println("GRE >");
                    break;
                case "<=":
                    System.out.println("LEQ <=");
                    break;
                case ">=":
                    System.out.println("GEQ >=");
                    break;
            }
            addExpNode.print();
        }
        System.out.println("<RelExp>");
    }
}
