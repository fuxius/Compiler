package token;
import token.Token;
import token.TokenType;
import java.util.HashMap;
import java.util.Map;


public class TokenFactory {
    private static TokenFactory instance = new TokenFactory();

    public Map<String, TokenType> getKeywords() {
        return keywords;
    }

    private Map<String, TokenType> keywords;

    private TokenFactory() {
        keywords = new HashMap<>();
        // 初始化关键字和标识符
        loadKeywords();
    }

    public static TokenFactory getInstance() {
        return instance;
    }

    private void loadKeywords() {
        keywords.put("main", TokenType.MAINTK);
        keywords.put("const", TokenType.CONSTTK);
        keywords.put("int", TokenType.INTTK);
        keywords.put("char", TokenType.CHARTK);
        keywords.put("break", TokenType.BREAKTK);
        keywords.put("continue", TokenType.CONTINUETK);
        keywords.put("if", TokenType.IFTK);
        keywords.put("else", TokenType.ELSETK);
        keywords.put("for", TokenType.FORTK);
        keywords.put("getint", TokenType.GETINTTK);
        keywords.put("getchar", TokenType.GETCHARTK);
        keywords.put("printf", TokenType.PRINTFTK);
        keywords.put("return", TokenType.RETURNTK);
        keywords.put("void", TokenType.VOIDTK);
        keywords.put("switch", TokenType.SWITCHTK);
        keywords.put("case", TokenType.CASETK);
        keywords.put("default", TokenType.DEFAULTTK);
        // 加载操作符
        keywords.put("!", TokenType.NOT);
        keywords.put("&&", TokenType.AND);
        keywords.put("||", TokenType.OR);
        keywords.put("+", TokenType.PLUS);
        keywords.put("-", TokenType.MINU);
        keywords.put("*", TokenType.MULT);
        keywords.put("/", TokenType.DIV);
        keywords.put("%", TokenType.MOD);
        keywords.put("=", TokenType.ASSIGN);
        keywords.put("==", TokenType.EQL);
        keywords.put("!=", TokenType.NEQ);
        keywords.put("<", TokenType.LSS);
        keywords.put("<=", TokenType.LEQ);
        keywords.put(">", TokenType.GRE);
        keywords.put(">=", TokenType.GEQ);
        // 加载分隔符
        keywords.put(";", TokenType.SEMICN);
        keywords.put(",", TokenType.COMMA);
        keywords.put("(", TokenType.LPARENT);
        keywords.put(")", TokenType.RPARENT);
        keywords.put("[", TokenType.LBRACK);
        keywords.put("]", TokenType.RBRACK);
        keywords.put("{", TokenType.LBRACE);
        keywords.put("}", TokenType.RBRACE);
    }

    // 处理标识符或关键字
    public Token createToken(String value, int line) {
        TokenType type = keywords.getOrDefault(value, TokenType.IDENFR);
        return new Token(type, value, line);
    }

    // 处理其他类型的Token
    public Token createToken(String value, int line, TokenType type) {
        return new Token(type, value, line);
    }
}
