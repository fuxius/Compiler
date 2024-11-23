package ast;

import token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量初值节点
 * 对应文法：
 * ConstInitVal → ConstExp
 *               | '{' [ ConstExp { ',' ConstExp } ] '}'
 *               | StringConst
 */
public class ConstInitValNode {
    private ConstExpNode constExpNode;
    private List<ConstExpNode> constExpNodeList;
    private String stringConst;
    private Token token;

    // 单个常量表达式
    public ConstInitValNode(ConstExpNode constExpNode) {
        this.constExpNode = constExpNode;
    }

    public Token getToken() {
        return token;
    }

    // 常量表达式列表（数组）
    public ConstInitValNode(List<ConstExpNode> constExpNodeList) {
        this.constExpNodeList = constExpNodeList;
    }

    // 字符串常量
    public ConstInitValNode(Token token) {
        this.token = token;
        this.stringConst = token.getValue();
    }
    /**
     * 计算常量初值的值
     *
     * @return 常量初值的值列表
     */
    /**
     * 计算常量初值的值
     *
     * @return 常量初值的值列表
     */
    public ArrayList<Integer> evaluate() {
        ArrayList<Integer> result = new ArrayList<>();

        if (constExpNode != null) {
            // 单个常量表达式
            result.add(constExpNode.evaluate());
        } else if (constExpNodeList != null) {
            // 常量表达式列表（数组）
            for (ConstExpNode expNode : constExpNodeList) {
                result.add(expNode.evaluate());
            }
        } else if (stringConst != null) {
            // 字符串常量
            result.addAll(evaluateStringConst());
        }

        return result;
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



    /**
     * 判断常量是否为零初始化
     *
     * @return 如果所有值均为 0，则返回 true；否则返回 false
     */
    public boolean isZero() {
        ArrayList<Integer> values = evaluate();
        for (int value : values) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }
    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public List<ConstExpNode> getConstExpNodeList() {
        return constExpNodeList;
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

    public void print() {
        if (constExpNode != null) {
            constExpNode.print();
        } else if (constExpNodeList != null) {
            System.out.println("LBRACE {");
            for (int i = 0; i < constExpNodeList.size(); i++) {
                constExpNodeList.get(i).print();
                if (i < constExpNodeList.size() - 1) {
                    System.out.println("COMMA ,");
                }
            }
            System.out.println("RBRACE }");
        } else if (stringConst != null) {
            System.out.println("STRCON " + stringConst);
        }

        System.out.println("<ConstInitVal>");
    }
}
