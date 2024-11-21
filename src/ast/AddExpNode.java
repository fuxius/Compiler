package ast;

import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * 加减表达式节点
 * 对应文法：AddExp → MulExp | AddExp ('+' | '−') MulExp
 */
public class AddExpNode {
    private AddExpNode addExpNode;
    private String op; // '+' 或 '-'
    private MulExpNode mulExpNode;
    private MulExpNode singleMulExpNode;
    private Token token;
    private List<MulExpNode> mulExpNodes = new ArrayList<>();  // 存储所有 MulExp 子节点
    private List<Token> operators = new ArrayList<>();        // 存储所有运算符
    private boolean listsPopulated = false; // 是否已经调用 populateLists

    public List<MulExpNode> getMulExpNodes() {
        return mulExpNodes;
    }
    public void addMulExp(MulExpNode mulExpNode) {
        mulExpNodes.add(mulExpNode);
    }

    public void addOperator(Token operator) {
        operators.add(operator);
    }
    public void setMulExpNodes(List<MulExpNode> mulExpNodes) {
        this.mulExpNodes = mulExpNodes;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void setOperators(List<Token> operators) {
        this.operators = operators;
    }

    // 单个 MulExp
    public AddExpNode(MulExpNode singleMulExpNode) {
        this.singleMulExpNode = singleMulExpNode;

    }

    // AddExp ('+' | '−') MulExp
    public AddExpNode(AddExpNode addExpNode, Token token, MulExpNode mulExpNode) {
        this.addExpNode = addExpNode;
        this.token = token;
        this.op = token.getValue();
        this.mulExpNode = mulExpNode;
    }

    public Token getToken() {
        return token;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public String getOp() {
        return op;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public MulExpNode getSingleMulExpNode() {
        return singleMulExpNode;
    }

    public void print() {
        if (singleMulExpNode != null) {
            singleMulExpNode.print();
        } else {
            addExpNode.print();
            if (op.equals("+")) {
                System.out.println("PLUS +");
            } else if (op.equals("-")) {
                System.out.println("MINU -");
            }
            mulExpNode.print();
        }
        System.out.println("<AddExp>");
    }

    public void populateLists() {
        // 如果已经调用过 populateLists，则直接返回
        if (listsPopulated) return;
        if (addExpNode != null) {
            addExpNode.populateLists(); // Recursively populate from previous node
            mulExpNodes.addAll(addExpNode.mulExpNodes);
            operators.addAll(addExpNode.operators);
        }
        if (singleMulExpNode != null) {
            // This is the deepest node, start the list here
            mulExpNodes.add(singleMulExpNode);
        } else {
            // Add the current operator and MulExp node
            operators.add(token);
            mulExpNodes.add(mulExpNode);
        }
        listsPopulated = true; // 标记为已填充
    }
    // 计算 AddExp 的值
    public int evaluate() {
        // 如果没有预先填充列表，则填充
        if (!listsPopulated) {
            populateLists();
        }

        // 初始操作数
        int result = mulExpNodes.get(0).evaluate();

        // 遍历操作符和操作数，计算结果
        for (int i = 0; i < operators.size(); i++) {
            Token operator = operators.get(i);
            int nextValue = mulExpNodes.get(i + 1).evaluate();
            if (operator.getType() == TokenType.PLUS) {
                result += nextValue;
            } else if (operator.getType() == TokenType.MINU) {
                result -= nextValue;
            }
        }

        return result;
    }
}
