package ast;

import token.Token;

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
    private Token token;
    private FuncRParamsNode funcRParamsNode; // 可选
    private UnaryOpNode unaryOpNode;
    private UnaryExpNode unaryExpNode;

    // PrimaryExp
    public UnaryExpNode(PrimaryExpNode primaryExpNode) {
        this.primaryExpNode = primaryExpNode;
    }

    public Token getToken() {
        return token;
    }

    // Ident '(' [FuncRParams] ')'
    public UnaryExpNode(Token token, FuncRParamsNode funcRParamsNode) {
        this.token = token;
        this.ident = token.getValue();
        this.funcRParamsNode = funcRParamsNode;
    }

    public PrimaryExpNode getPrimaryExpNode() {
        return primaryExpNode;
    }

    public String getIdent() {
        return ident;
    }

    public FuncRParamsNode getFuncRParamsNode() {
        return funcRParamsNode;
    }

    public UnaryOpNode getUnaryOpNode() {
        return unaryOpNode;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
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
    // 计算 UnaryExp 的值
    public int evaluate() {
        // 如果是基本表达式
        if (primaryExpNode != null) {
            return primaryExpNode.evaluate();
        }
        // 如果是函数调用（暂不支持函数调用的值计算，返回默认值0）
        else if (ident != null) {
            return 0; // 这里可以根据上下文支持函数返回值计算
        }
        // 如果是 UnaryOp UnaryExp
        else if (unaryOpNode != null && unaryExpNode != null) {
            int operand = unaryExpNode.evaluate();
            String op = unaryOpNode.getToken().getValue(); // 获取操作符

            if (op.equals("+")) {
                return operand; // 一元加号，不改变值
            } else if (op.equals("-")) {
                return -operand; // 一元减号，取负值
            } else if (op.equals("!")) {
                return operand == 0 ? 1 : 0; // 逻辑非，0 -> 1，非0 -> 0
            }
        }
        return 0; // 默认返回值
    }
}
