package ast;

import token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量初值节点
 * 对应文法：
 * InitVal → Exp
 *            | '{' [ Exp { ',' Exp } ] '}'
 *            | StringConst
 */
public class InitValNode {
    private ExpNode expNode;
    private List<ExpNode> expNodeList;
    private String stringConst;
    private Token token;

    // 单个表达式
    public InitValNode(ExpNode expNode) {
        this.expNode = expNode;
    }

    // 表达式列表（数组）
    public InitValNode(List<ExpNode> expNodeList) {
        this.expNodeList = expNodeList;
    }

    public Token getToken() {
        return token;
    }

    // 字符串常量
    public InitValNode(Token token) {
        this.token = token;
        this.stringConst = token.getValue();
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public List<ExpNode> getExpNodeList() {
        return expNodeList;
    }
    //判断是否为字符串常量
    public boolean isStringConst() {
        return stringConst != null;
    }
    public String getStringConst() {
        if(stringConst == null)     return null;
        String strippedString = stringConst;

        // 去掉字符串两侧的引号
        if (stringConst.length() > 1 && stringConst.charAt(0) == '\"' && stringConst.charAt(stringConst.length() - 1) == '\"') {
            strippedString = stringConst.substring(1, stringConst.length() - 1);
        }
        return strippedString;
    }

    public boolean isZero() {
        ArrayList<Integer> values = evaluate();

        // 检查所有值是否均为 0
        for (int value : values) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    // 评估初始值
    public ArrayList<Integer> evaluate() {
        ArrayList<Integer> values = new ArrayList<>();

        // 如果是单个表达式
        if (expNode != null) {
            values.add(expNode.evaluate()); // 调用 ExpNode 的 evaluate 方法
        }
        // 如果是表达式列表
        else if (expNodeList != null) {
            for (ExpNode exp : expNodeList) {
                values.add(exp.evaluate()); // 对每个表达式求值
            }
        }
        // 如果是字符串常量
        else if (stringConst != null) {
            values.addAll(evaluateStringConst()); // 调用辅助方法处理字符串常量
        }

        return values;
    }
    /**
     * 处理字符串常量，将其拆分为 ASCII 值，并处理转义字符。
     *
     * @return 字符串常量的 ASCII 值列表
     */
    private ArrayList<Integer> evaluateStringConst() {
        ArrayList<Integer> values = new ArrayList<>();
        String strippedString = stringConst;

        // 去掉字符串两侧的引号
        if (stringConst.length() > 1 && stringConst.charAt(0) == '\"' && stringConst.charAt(stringConst.length() - 1) == '\"') {
            strippedString = stringConst.substring(1, stringConst.length() - 1);
        }

        // 遍历字符串并处理字符和转义字符
        for (int i = 0; i < strippedString.length(); i++) {
            char c = strippedString.charAt(i);
            if (c == '\\' && i + 1 < strippedString.length()) { // 检测到转义字符
                char nextChar = strippedString.charAt(++i);
                switch (nextChar) {
                    case 'a': c = '\u0007'; break; // 警报字符
                    case 'b': c = '\b';    break; // 退格
                    case 't': c = '\t';    break; // 水平制表符
                    case 'n': c = '\n';    break; // 换行
                    case 'v': c = '\u000B'; break; // 垂直制表符
                    case 'f': c = '\f';    break; // 换页
                    case 'r': c = '\r';    break; // 回车
                    case '\\': c = '\\';   break; // 反斜杠
                    case '\'': c = '\'';   break; // 单引号
                    case '"': c = '\"';    break; // 双引号
                    case '0': c = '\0';    break; // 空字符
                    default: throw new IllegalArgumentException("Unsupported escape sequence: \\" + nextChar);
                }
            }
            values.add((int) c);
        }

        // 添加结束符 '\0'
        values.add(0);

        return values;
    }


    public void print() {
        if (expNode != null) {
            expNode.print();
        } else if (expNodeList != null) {
            System.out.println("LBRACE {");
            for (int i = 0; i < expNodeList.size(); i++) {
                expNodeList.get(i).print();
                if (i < expNodeList.size() - 1) {
                    System.out.println("COMMA ,");
                }
            }
            System.out.println("RBRACE }");
        } else if (stringConst != null) {
            System.out.println("STRCON " + stringConst);
        }
        System.out.println("<InitVal>");
    }
}
