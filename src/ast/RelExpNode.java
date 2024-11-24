package ast;

import token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 关系表达式节点
 * 对应文法：RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
 */
public class RelExpNode {
    private RelExpNode relExpNode;
    private String op; // '<', '>', '<=', '>='
    private Token token;
    private AddExpNode addExpNode;
    private AddExpNode singleAddExpNode;
    private boolean listsPopulated  = false; // 是否已经调用 populateLists
    private List<AddExpNode> addExpNodes = new ArrayList<>();  // 存储所有 AddExp 子节点
    private List<Token> operators = new ArrayList<>();        // 存储所有运算符
    // 单个 AddExp
    public RelExpNode(AddExpNode singleAddExpNode) {
        this.singleAddExpNode = singleAddExpNode;
    }

    // RelExp ('<' | '>' | '<=' | '>=') AddExp
    public RelExpNode(RelExpNode relExpNode, Token token, AddExpNode addExpNode) {
        this.relExpNode = relExpNode;
        this.token = token;
        this.op = token.getValue();
        this.addExpNode = addExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public String getOp() {
        return op;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public AddExpNode getSingleAddExpNode() {
        return singleAddExpNode;
    }

    public void populateLists() {
        // 如果已经调用过 populateLists，则直接返回
        if (listsPopulated) return;
        if (relExpNode != null) {
            relExpNode.populateLists(); // Recursively populate from previous node
            addExpNodes.addAll(relExpNode.addExpNodes);
            operators.addAll(relExpNode.operators);
        }
        if (singleAddExpNode != null) {
            // This is the deepest node, start the list here
            addExpNodes.add(singleAddExpNode);
        } else {
            // Add the current operator and AddExp node
            operators.add(token);
            addExpNodes.add(addExpNode);
        }
        listsPopulated = true; // 标记为已填充
    }

    public List<AddExpNode> getAddExpNodes() {
        return addExpNodes;
    }

    public List<Token> getOperators() {
        return operators;
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
