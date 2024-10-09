package token;
public enum TokenType {
    // 标识符和常量
    IDENFR,     // Ident
    INTCON,     // IntConst
    STRCON,     // StringConst
    CHRCON,     // CharConst

    // 保留字
    MAINTK,     // main
    CONSTTK,    // const
    INTTK,      // int
    CHARTK,     // char
    BREAKTK,    // break
    CONTINUETK, // continue
    IFTK,       // if
    ELSETK,     // else
    FORTK,      // for
    GETINTTK,   // getint
    GETCHARTK,  // getchar
    PRINTFTK,   // printf
    RETURNTK,   // return
    VOIDTK,     // void

    // 操作符
    NOT,        // !
    AND,        // &&
    OR,         // ||
    PLUS,       // +
    MINU,       // -
    MULT,       // *
    DIV,        // /
    MOD,        // %
    ASSIGN,     // =
    EQL,        // ==
    NEQ,        // !=
    LSS,        // <
    LEQ,        // <=
    GRE,        // >
    GEQ,        // >=

    // 分隔符
    SEMICN,     // ;
    COMMA,      // ,
    LPARENT,    // (
    RPARENT,    // )
    LBRACK,     // [
    RBRACK,     // ]
    LBRACE,     // {
    RBRACE,     // }

    // 文件结束
    EOF ,        // End of file
    UNKNOWN     // 未知类型，用于错误处理
}
