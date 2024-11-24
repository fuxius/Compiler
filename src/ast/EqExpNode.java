package ast;

import token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 相等性表达式节点
 * 对应文法：EqExp → RelExp | EqExp ('==' | '!=') RelExp
 */
public class EqExpNode {
    private EqExpNode eqExpNode;
    private String op; // '==' 或 '!='
    private RelExpNode relExpNode;
    private RelExpNode singleRelExpNode;
    private Token token;
    private boolean listsPopulated  = false; // 是否已经调用 populateLists
    private List<RelExpNode> relExpNodes = new ArrayList<>();  // 存储所有 RelExp 子节点
    private List<Token> operators = new ArrayList<>();        // 存储所有运算符

    // 单个 RelExp
    public EqExpNode(RelExpNode singleRelExpNode) {
        this.singleRelExpNode = singleRelExpNode;
    }

    public Token getToken() {
        return token;
    }

    // EqExp ('==' | '!=') RelExp
    public EqExpNode(EqExpNode eqExpNode, Token token, RelExpNode relExpNode) {
        this.eqExpNode = eqExpNode;
        this.token = token;
        this.op = token.getValue();
        this.relExpNode = relExpNode;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public String getOp() {
        return op;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public RelExpNode getSingleRelExpNode() {
        return singleRelExpNode;
    }

    public List<RelExpNode> getRelExpNodes() {
        return relExpNodes;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void populateLists() {
        // 如果已经调用过 populateLists，则直接返回
        if (listsPopulated) return;
        if (eqExpNode != null) {
            eqExpNode.populateLists(); // Recursively populate from previous node
            relExpNodes.addAll(eqExpNode.relExpNodes);
            operators.addAll(eqExpNode.operators);
        }
        if (singleRelExpNode != null) {
            // This is the deepest node, start the list here
            relExpNodes.add(singleRelExpNode);
        } else {
            // Add the current operator and RelExp node
            operators.add(token);
            relExpNodes.add(relExpNode);
        }
        listsPopulated = true; // 标记为已填充
    }
    public void print() {
        if (singleRelExpNode != null) {
            singleRelExpNode.print();
        } else {
            eqExpNode.print();
            if (op.equals("==")) {
                System.out.println("EQL ==");
            } else if (op.equals("!=")) {
                System.out.println("NEQ !=");
            }
            relExpNode.print();
        }
        System.out.println("<EqExp>");
    }
}
