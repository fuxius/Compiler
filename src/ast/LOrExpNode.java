package ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑或表达式节点
 * 对应文法：LOrExp → LAndExp | LOrExp '||' LAndExp
 */
public class LOrExpNode {
    private LOrExpNode lOrExpNode;
    private LAndExpNode lAndExpNode;
    private LAndExpNode singleLAndExpNode;
    private boolean listsPopulated  = false; // 是否已经调用 populateLists
    private List<LAndExpNode> lAndExpNodes = new ArrayList<>();  // 存储所有 LAndExp 子节点
    private List<String> operators = new ArrayList<>();        // 存储所有运算符
    // 单个 LAndExp
    public LOrExpNode(LAndExpNode singleLAndExpNode) {
        this.singleLAndExpNode = singleLAndExpNode;
    }

    // LOrExp '||' LAndExp
    public LOrExpNode(LOrExpNode lOrExpNode, LAndExpNode lAndExpNode) {
        this.lOrExpNode = lOrExpNode;
        this.lAndExpNode = lAndExpNode;
    }

    public List<LAndExpNode> getlAndExpNodes() {
        return lAndExpNodes;
    }

    public List<String> getOperators() {
        return operators;
    }

    public LOrExpNode getlOrExpNode() {
        return lOrExpNode;
    }

    public LAndExpNode getlAndExpNode() {
        return lAndExpNode;
    }

    public LAndExpNode getSingleLAndExpNode() {
        return singleLAndExpNode;
    }

    public void populateLists() {
        // 如果已经调用过 populateLists，则直接返回
        if (listsPopulated) return;
        if (lOrExpNode != null) {
            lOrExpNode.populateLists(); // Recursively populate from previous node
            lAndExpNodes.addAll(lOrExpNode.lAndExpNodes);
            operators.addAll(lOrExpNode.operators);
        }
        if (singleLAndExpNode != null) {
            // This is the deepest node, start the list here
            lAndExpNodes.add(singleLAndExpNode);
        } else {
            // Add the current operator and LAndExp node
            operators.add("||");
            lAndExpNodes.add(lAndExpNode);
        }
        listsPopulated = true; // 标记为已填充
    }
    public void print() {
        if (singleLAndExpNode != null) {
            singleLAndExpNode.print();
        } else {
            lOrExpNode.print();
            System.out.println("OR ||");
            lAndExpNode.print();
        }
        System.out.println("<LOrExp>");

    }
}
