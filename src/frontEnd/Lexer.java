package frontEnd;

import error.ErrorType;
import token.Token;
import token.TokenFactory;
import error.ErrorHandler;
import token.TokenManager;
import token.TokenType;
import util.IOUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer类用于将输入的源代码转换为Token。
 */
public class Lexer {
    private static Lexer instance = new Lexer(); // Lexer单例对象

    private TokenManager tokenManager = TokenManager.getInstance();  // TokenManager单例对象
    private int line = 1;                             // 当前行号
    private String content;                           // 输入的源代码
    private int position = 0;                         // 当前读取到的字符位置

    /**
     * 获取Lexer单例对象。
     * @return Lexer单例对象
     */
    public static Lexer getInstance() {
        return instance;
    }

    /**
     * 主分析函数，将输入的源代码转换为Token。
     * @param content 输入的源代码
     */
    public void analyze(String content) {
        this.content = content;
        while (position < content.length()) {
            char currentChar = content.charAt(position);

            if (Character.isWhitespace(currentChar)) { // 处理空白字符
                if (currentChar == '\n') {
                    line++;
                }
                position++;
            } else if (Character.isLetter(currentChar) || currentChar == '_') { // 处理标识符或关键字
                analyzeIdentifierOrKeyword();
            } else if (Character.isDigit(currentChar)) { // 处理数字
                analyzeNumber();
            } else if (currentChar == '"') { // 处理字符串
                analyzeString();
            } else if (currentChar == '\'') { // 处理字符
                analyzeChar();
            } else if (isSingleCharDelimiter(currentChar)) { // 处理单字符分隔符
                analyzeSingleCharDelimiter();
            } else if (currentChar == '/') { // 处理注释或除号
                analyzeCommentOrDivide();
            } else if (currentChar == '&' || currentChar == '|') { // 处理逻辑运算符
                analyzeLogicalOperators();
            } else {
                // 遇到无法识别的符号
                position++;
            }
        }
        saveResults(); // 保存Token和错误到文件
    }

    /**
     * 处理标识符或关键字。
     */
    private void analyzeIdentifierOrKeyword() {
        StringBuilder tokenBuilder = new StringBuilder();
        char currentChar;
        while (position < content.length() && (Character.isLetterOrDigit(currentChar = content.charAt(position)) || currentChar == '_')) {
            tokenBuilder.append(currentChar);
            position++;
        }
        String tokenStr = tokenBuilder.toString();
        Token token = TokenFactory.getInstance().createToken(tokenStr, line);
        tokenManager.saveToken(token);
    }

    /**
     * 处理数字。
     */
    private void analyzeNumber() {
        StringBuilder numberBuilder = new StringBuilder();
        char currentChar;
        while (position < content.length() && Character.isDigit(currentChar = content.charAt(position))) {
            numberBuilder.append(currentChar);
            position++;
        }
        String numberStr = numberBuilder.toString();
        tokenManager.saveToken(TokenFactory.getInstance().createToken(numberStr, line, TokenType.INTCON));
    }

    /**
     * 处理字符串。
     */
    private void analyzeString() {
        position++; // 跳过起始的引号
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('"');
        char currentChar;
        while (position < content.length() && (currentChar = content.charAt(position)) != '"') {
            stringBuilder.append(currentChar);
            position++;
        }
        if (position < content.length() && content.charAt(position) == '"') {
            position++; // 跳过结束引号
            stringBuilder.append('"');
            tokenManager.saveToken(TokenFactory.getInstance().createToken(stringBuilder.toString(), line, TokenType.STRCON));
        }
    }

    /**
     * 处理字符。
     */
    private void analyzeChar() {
        position++; // 跳过起始的单引号
        if (position < content.length()) {
            StringBuilder charBuilder = new StringBuilder();
            charBuilder.append("'");
            char charContent = content.charAt(position);

            if (charContent == '\\') { // 处理转义字符
                position++;
                if (position < content.length()) {
                    charContent = content.charAt(position);
                    switch (charContent) {
                        case '0': charBuilder.append("\\0"); break;
                        case 'a': charBuilder.append("\\a"); break;
                        case 'b': charBuilder.append("\\b"); break;
                        case 't': charBuilder.append("\\t"); break;
                        case 'n': charBuilder.append("\\n"); break;
                        case 'v': charBuilder.append("\\v"); break;
                        case 'f': charBuilder.append("\\f"); break;
                        case '\\': charBuilder.append("\\\\"); break;
                        case '\'': charBuilder.append("\\'"); break;
                        case '"': charBuilder.append("\\\""); break;
                        default:
                            // 如果未知的转义字符，保持原样输出并报错
                            charBuilder.append("\\").append(charContent);
                            break;
                    }
                    position++;
                }
            } else {
                charBuilder.append(charContent);
                position++;
            }

            if (position < content.length() && content.charAt(position) == '\'') {
                position++; // 跳过结束单引号
                charBuilder.append("'");
                tokenManager.saveToken(TokenFactory.getInstance().createToken(charBuilder.toString(), line, TokenType.CHRCON));
            }
        }
    }

    /**
     * 处理单字符分隔符。
     */
    private void analyzeSingleCharDelimiter() {
        StringBuilder stringBuilder = new StringBuilder();
        char currentChar = content.charAt(position);
        position++;
        if(position < content.length() && "!<>=".indexOf(currentChar) >= 0) {
                char nextChar = content.charAt(position);
                if(nextChar == '=') {
                    stringBuilder.append(currentChar).append(nextChar);
                    tokenManager.saveToken(TokenFactory.getInstance().createToken(stringBuilder.toString(), line));
                    position++;
                }else {
                    tokenManager.saveToken(TokenFactory.getInstance().createToken(String.valueOf(currentChar), line));
                }
        }
        else {
            tokenManager.saveToken(TokenFactory.getInstance().createToken(String.valueOf(currentChar), line));
        }
    }

    /**
     * 处理注释或除号。
     */
    private void analyzeCommentOrDivide() {
        position++;  // 假设已经遇到了第一个 '/'
        if (position < content.length()) {
            char nextChar = content.charAt(position);
            if (nextChar == '/') {
                // 处理单行注释
                position++;  // 跳过第二个 '/'
                while (position < content.length() && content.charAt(position) != '\n') {
                    position++;  // 继续前进直到行末
                }
                // 这里不需要创建Token，单行注释忽略掉
            } else if (nextChar == '*') {
                // 处理多行注释
                position++;  // 跳过 '*'
                boolean foundEnd = false;
                while (position < content.length()) {
                    char currentChar = content.charAt(position);
                    if (currentChar == '*' && position + 1 < content.length() && content.charAt(position + 1) == '/') {
                        position += 2;  // 跳过结束的 "*/"
                        foundEnd = true;
                        break;
                    }
                    if (currentChar == '\n') {
                        line++;  // 多行注释中的换行符也要计算行号
                    }
                    position++;
                }
            } else {
                // 不是注释，而是除号
                tokenManager.saveToken(TokenFactory.getInstance().createToken("/", line, TokenType.DIV));
            }
        }
    }

    /**
     * 处理逻辑运算符。
     */
    private void analyzeLogicalOperators() {
        char currentChar = content.charAt(position);
        position++;
        if (position < content.length() && content.charAt(position) == currentChar) {
            if (currentChar == '&') {
                tokenManager.saveToken(TokenFactory.getInstance().createToken("&&", line, TokenType.AND));
            } else if (currentChar == '|') {
                tokenManager.saveToken(TokenFactory.getInstance().createToken("||", line, TokenType.OR));
            }
            position++;
        } else {
            // 处理非法符号 & 或 |
            ErrorHandler.getInstance().reportError(line, ErrorType.ILLEGAL_SYMBOL);
            if(currentChar == '&'){
                tokenManager.saveToken(TokenFactory.getInstance().createToken("&&", line, TokenType.AND));
            }else {
                tokenManager.saveToken(TokenFactory.getInstance().createToken("||", line, TokenType.OR));
            }
        }
    }

    /**
     * 判断字符是否为单字符分隔符。
     * @param c 要判断的字符
     * @return 如果是单字符分隔符则返回true，否则返回false
     */
    private boolean isSingleCharDelimiter(char c) {
        return "+-*%(){}[],;!<>=".indexOf(c) >= 0;
    }

    /**
     * 保存Token和错误到文件。
     */
    private void saveResults() {
        IOUtils.writeTokensToFile(tokenManager.getTokens(), "lexer.txt");
    }
}