package ast;

import token.Token;

import java.util.List;
/**
 * 语句节点
 * 对应文法：
 * Stmt → LVal '=' Exp ';' // i
 * | [Exp] ';' // i
 * | Block
 * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
 * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
 * | 'break' ';' | 'continue' ';' // i
 * | 'return' [Exp] ';' // i
 * | LVal '=' 'getint''('')'';' // i j
 * | LVal '=' 'getchar''('')'';' // i j
 * | 'printf''('StringConst {','Exp}')'';' // i j
 */
public class StmtNode {
    // 不同类型的语句，可以根据需要添加更多字段
    private StmtType stmtType;
    private LValNode lValNode;
    private ExpNode expNode;
    private BlockNode blockNode;
    private StmtNode stmtNode1;
    private StmtNode stmtNode2;
    private String keyword; // 'break'、'continue'、'return' 等
    private Token token;
    private String stringConst;
    private List<ExpNode> expNodeList;
    private CondNode condNode;
    private ForStmtNode forStmtNode1;
    private ForStmtNode forStmtNode2;

    // 构造方法根据不同的语句类型进行重载
    // 示例：LVal '=' Exp ';'
    public StmtNode(LValNode lValNode, ExpNode expNode) {
        this.lValNode = lValNode;
        this.expNode = expNode;
        this.stmtType = StmtType.ASSIGN;
    }
    public StmtNode() {
        // 初始化字段（如果需要）
        this.stmtType = StmtType.NULL;
    }
    // 示例：[Exp] ';'
    public StmtNode(ExpNode expNode) {
        this.stmtType = StmtType.EXP;
        this.expNode = expNode;
    }

    // 示例：Block
    public StmtNode(BlockNode blockNode) {
        this.stmtType = StmtType.BLOCK;
        this.blockNode = blockNode;
    }

    // 示例：'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public StmtNode(CondNode condNode, StmtNode stmtNode1, StmtNode stmtNode2) {
        this.stmtType = StmtType.IF;
        this.condNode = condNode;
        this.stmtNode1 = stmtNode1;
        this.stmtNode2 = stmtNode2;
    }

    // 示例：'break' ';' 或 'continue' ';'
    public StmtNode(Token token) {
        this.stmtType = StmtType.BREAKorCONTINUE;
        this.token = token;
        this.keyword = token.getValue();
    }

    public Token getToken() {
        return token;
    }

    // 示例：'return' [Exp] ';'
    public StmtNode(Token token, ExpNode expNode) {
        this.token= token;
        this.keyword = token.getValue();
        this.expNode = expNode;
        this.stmtType = StmtType.RETURN;
    }

    // 示例：LVal '=' 'getint' '(' ')' ';'
    public StmtNode(LValNode lValNode, String functionName) {
        this.lValNode = lValNode;
        this.keyword = functionName;
        this.stmtType = StmtType.GET;
    }

    // 示例：'printf' '(' StringConst {',' Exp} ')' ';'
    public StmtNode(Token token, List<ExpNode> expNodeList) {
        this.token = token;
        this.stringConst = token.getValue();
        this.expNodeList = expNodeList;
        this.stmtType = StmtType.PRINTF;
    }

    // 示例：'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    public StmtNode(ForStmtNode forStmtNode1, CondNode condNode, ForStmtNode forStmtNode2, StmtNode stmtNode1) {
        this.forStmtNode1 = forStmtNode1;
        this.condNode = condNode;
        this.forStmtNode2 = forStmtNode2;
        this.stmtNode1 = stmtNode1;
        this.stmtType = StmtType.FOR;
    }
    public StmtType getStmtType() {
        return stmtType;
    }
    public LValNode getlValNode() {
        return lValNode;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public StmtNode getStmtNode1() {
        return stmtNode1;
    }

    public StmtNode getStmtNode2() {
        return stmtNode2;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getStringConst() {
        return stringConst;
    }

    public List<ExpNode> getExpNodeList() {
        return expNodeList;
    }

    public CondNode getCondNode() {
        return condNode;
    }

    public ForStmtNode getForStmtNode1() {
        return forStmtNode1;
    }

    public ForStmtNode getForStmtNode2() {
        return forStmtNode2;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }
    public void print() {
        if (lValNode != null && expNode != null && keyword == null) {
            // LVal '=' Exp ';'
            lValNode.print();
            System.out.println("ASSIGN =");
            expNode.print();
            System.out.println("SEMICN ;");
            System.out.println("<Stmt>");
        } else if (expNode != null && lValNode == null && blockNode == null) {
            // [Exp] ';'
            expNode.print();
            System.out.println("SEMICN ;");
            System.out.println("<Stmt>");
        } else if (blockNode != null) {
            // Block
            blockNode.print();
            // 不需要输出 <Stmt>，因为在 BlockNode 中已经输出了 <Block>
        } else if (keyword != null && (keyword.equals("break") || keyword.equals("continue"))) {
            // 'break' ';' | 'continue' ';'
            System.out.println(keyword.toUpperCase() + "TK " + keyword);
            System.out.println("SEMICN ;");
            System.out.println("<Stmt>");
        } else if (keyword != null && keyword.equals("return")) {
            // 'return' [Exp] ';'
            System.out.println("RETURNTK return");
            if (expNode != null) {
                expNode.print();
            }
            System.out.println("SEMICN ;");
            System.out.println("<Stmt>");
        } else if (lValNode != null && keyword != null && (keyword.equals("getint") || keyword.equals("getchar"))) {
            // LVal '=' 'getint' '(' ')' ';' | LVal '=' 'getchar' '(' ')' ';'
            lValNode.print();
            System.out.println("ASSIGN =");
            System.out.println(keyword.toUpperCase() + "TK " + keyword);
            System.out.println("LPARENT (");
            System.out.println("RPARENT )");
            System.out.println("SEMICN ;");
            System.out.println("<Stmt>");
        } else if (stringConst != null) {
            // 'printf' '(' StringConst {',' Exp} ')' ';'
            System.out.println("PRINTFTK printf");
            System.out.println("LPARENT (");
            System.out.println("STRCON " + stringConst);
            for (ExpNode expNode : expNodeList) {
                System.out.println("COMMA ,");
                expNode.print();
            }
            System.out.println("RPARENT )");
            System.out.println("SEMICN ;");
            System.out.println("<Stmt>");
        } else if (condNode != null && stmtNode1 != null && stmtNode2 != null) {
            // 'if' '(' Cond ')' Stmt 'else' Stmt
            System.out.println("IFTK if");
            System.out.println("LPARENT (");
            condNode.print();
            System.out.println("RPARENT )");
            stmtNode1.print();
            System.out.println("ELSETK else");
            stmtNode2.print();
            System.out.println("<Stmt>");
        } else if (condNode != null && stmtNode1 != null) {
            // 'if' '(' Cond ')' Stmt
            System.out.println("IFTK if");
            System.out.println("LPARENT (");
            condNode.print();
            System.out.println("RPARENT )");
            stmtNode1.print();
            System.out.println("<Stmt>");
        } else if (forStmtNode1 != null && stmtNode1 != null) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            System.out.println("FORTK for");
            System.out.println("LPARENT (");
            if (forStmtNode1 != null) {
                forStmtNode1.print();
            }
            System.out.println("SEMICN ;");
            if (condNode != null) {
                condNode.print();
            }
            System.out.println("SEMICN ;");
            if (forStmtNode2 != null) {
                forStmtNode2.print();
            }
            System.out.println("RPARENT )");
            stmtNode1.print();
            System.out.println("<Stmt>");
        }
        // 添加更多的情况
    }

}
