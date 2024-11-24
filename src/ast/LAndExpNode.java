package ast;

import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑与表达式节点
 * 对应文法：LAndExp → EqExp | LAndExp '&&' EqExp
 */
public class LAndExpNode {
    private LAndExpNode lAndExpNode;
    private EqExpNode eqExpNode;
    private EqExpNode singleEqExpNode;
    private boolean listsPopulated  = false; // 是否已经调用 populateLists
    private List<EqExpNode> eqExpNodes = new ArrayList<>();  // 存储所有 EqExp 子节点
    private List<String> operators = new ArrayList<>();        // 存储所有运算符

    // 单个 EqExp
    public LAndExpNode(EqExpNode singleEqExpNode) {
        this.singleEqExpNode = singleEqExpNode;
    }

    // LAndExp '&&' EqExp
    public LAndExpNode(LAndExpNode lAndExpNode, EqExpNode eqExpNode) {
        this.lAndExpNode = lAndExpNode;
        this.eqExpNode = eqExpNode;
    }

    public LAndExpNode getlAndExpNode() {
        return lAndExpNode;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public EqExpNode getSingleEqExpNode() {
        return singleEqExpNode;
    }

    public List<EqExpNode> getEqExpNodes() {
        return eqExpNodes;
    }
    public List<String> getOperators() {
        return operators;
    }

    public void populateLists() {
        // 如果已经调用过 populateLists，则直接返回
        if (listsPopulated) return;
        if (lAndExpNode != null) {
            lAndExpNode.populateLists(); // Recursively populate from previous node
            eqExpNodes.addAll(lAndExpNode.eqExpNodes);
            operators.addAll(lAndExpNode.operators);
        }
        if (singleEqExpNode != null) {
            // This is the deepest node, start the list here
            eqExpNodes.add(singleEqExpNode);
        } else {
            // Add the current operator and EqExp node
            operators.add("&&");
            eqExpNodes.add(eqExpNode);
        }
        listsPopulated = true; // 标记为已填充
    }
    public void print() {
        if (singleEqExpNode != null) {
            singleEqExpNode.print();
        } else {
            lAndExpNode.print();
            System.out.println("AND &&");
            eqExpNode.print();
        }
        System.out.println("<LAndExp>");
    }
}
