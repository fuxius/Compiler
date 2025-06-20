package ast;

/**
 * 基本表达式节点
 * 对应文法：
 * PrimaryExp → '(' Exp ')' | LVal | Number | Character
 */
public class PrimaryExpNode {
    private ExpNode expNode;
    private LValNode lValNode;
    private NumberNode numberNode;
    private CharacterNode characterNode;

    // 构造方法根据不同的情况重载
    // '(' Exp ')'
    public PrimaryExpNode(ExpNode expNode) {
        this.expNode = expNode;
    }

    // LVal
    public PrimaryExpNode(LValNode lValNode) {
        this.lValNode = lValNode;
    }

    // Number
    public PrimaryExpNode(NumberNode numberNode) {
        this.numberNode = numberNode;
    }

    // Character
    public PrimaryExpNode(CharacterNode characterNode) {
        this.characterNode = characterNode;
    }

    public CharacterNode getCharacterNode() {
        return characterNode;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public LValNode getlValNode() {
        return lValNode;
    }

    public NumberNode getNumberNode() {
        return numberNode;
    }

    public void print() {
        if (expNode != null) {
            System.out.println("LPARENT (");
            expNode.print();
            System.out.println("RPARENT )");
        } else if (lValNode != null) {
            lValNode.print();
        } else if (numberNode != null) {
            numberNode.print();
        } else if (characterNode != null) {
            characterNode.print();
        }
        System.out.println("<PrimaryExp>");
    }
    /**
     * 计算 PrimaryExp 的值
     * PrimaryExp → '(' Exp ')' | LVal | Number | Character
     */
    public int evaluate() {
        // '(' Exp ')'
        if (expNode != null) {
            return expNode.evaluate();
        }
        // LVal
        else if (lValNode != null) {
            return lValNode.evaluate();
        }
        // Number
        else if (numberNode != null) {
            return numberNode.evaluate(); // 获取数字常量的值
        }
        // Character
        else if (characterNode != null) {
            return characterNode.evaluate(); // 获取字符常量的 ASCII 值
        }
        return 0; // 默认返回 0
    }
}
