package ast;

import token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 乘除模表达式节点
 * 对应文法：MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
 */
public class MulExpNode {
    private MulExpNode mulExpNode;
    private String op; // '*', '/', '%'
    private UnaryExpNode unaryExpNode;
    private UnaryExpNode singleUnaryExpNode;
    private Token token;
    private List<UnaryExpNode> unaryExpNodes = new ArrayList<>();  // 存储所有 UnaryExp 子节点
    private List<Token> operators = new ArrayList<>();            // 存储所有运算符
    private boolean listsPopulated = false; // 是否已经调用 populateLists

    public List<UnaryExpNode> getUnaryExpNodes() {
        return unaryExpNodes;
    }

    public List<Token> getOperators() {
        return operators;
    }

    // 单个 UnaryExp
    public MulExpNode(UnaryExpNode singleUnaryExpNode) {
        this.singleUnaryExpNode = singleUnaryExpNode;
    }

    public Token getToken() {
        return token;
    }
    public void addUnaryExp(UnaryExpNode unaryExpNode) {
        unaryExpNodes.add(unaryExpNode);
    }

    public void addOperator(Token operator) {
        operators.add(operator);
    }
    // MulExp ('*' | '/' | '%') UnaryExp
    public MulExpNode(MulExpNode mulExpNode, Token token, UnaryExpNode unaryExpNode) {
        this.mulExpNode = mulExpNode;
        this.token= token;
        this.op = token.getValue();
        this.unaryExpNode = unaryExpNode;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public String getOp() {
        return op;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public UnaryExpNode getSingleUnaryExpNode() {
        return singleUnaryExpNode;
    }

    public void print() {
        if (singleUnaryExpNode != null) {
            singleUnaryExpNode.print();
        } else {
            mulExpNode.print();
            if (op.equals("*")) {
                System.out.println("MULT *");
            } else if (op.equals("/")) {
                System.out.println("DIV /");
            } else if (op.equals("%")) {
                System.out.println("MOD %");
            }
            unaryExpNode.print();
        }
        System.out.println("<MulExp>");
    }
    public void populateLists() {
        // 如果已经调用过 populateLists，则直接返回
        if (listsPopulated) return;
        if (mulExpNode != null) {
            mulExpNode.populateLists(); // Recursively populate from previous node
            unaryExpNodes.addAll(mulExpNode.unaryExpNodes);
            operators.addAll(mulExpNode.operators);
        }
        if (singleUnaryExpNode != null) {
            // This is the deepest node, start the list here
            unaryExpNodes.add(singleUnaryExpNode);
        } else {
            // Add the current operator and UnaryExp node
            operators.add(token);
            unaryExpNodes.add(unaryExpNode);
        }
        listsPopulated = true; // 标记为已填充
    }
    // 计算 MulExp 的值
    public int evaluate() {
        // 如果列表尚未填充，先填充
        if (!listsPopulated) {
            populateLists();
        }

        // 初始操作数
        int result = unaryExpNodes.get(0).evaluate();

        // 遍历操作符和操作数，依次计算结果
        for (int i = 0; i < operators.size(); i++) {
            Token operator = operators.get(i);
            int nextValue = unaryExpNodes.get(i + 1).evaluate();
            switch (operator.getType()) {
                case MULT -> result *= nextValue; // 乘法
                case DIV -> {
                    result /= nextValue; // 除法
                }
                case MOD -> {
                    result %= nextValue; // 取模
                }
            }
        }

        return result;
    }
}
