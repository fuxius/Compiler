package ast;


public enum StmtType {
    ASSIGN,             // LVal '=' Exp ';'
    EXP,                // [Exp] ';'
    BLOCK,              // Block
    IF,                 // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    FOR,                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    BREAKorCONTINUE,    // 'break' ';' | 'continue' ';'
    RETURN,             // 'return' [Exp] ';'
    GET,                // LVal '=' 'getint' '(' ')' ';' | LVal '=' 'getchar' '(' ')' ';'
    PRINTF,              // 'printf' '(' StringConst {','Exp} ')' ';'
    // 根据需要添加其他类型
    NULL
}
