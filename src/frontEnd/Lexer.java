package frontEnd;

import error.ErrorType;
import token.Token;
import token.TokenFactory;
import error.ErrorHandler;
import token.TokenType;
import util.IOUtils;

import java.util.ArrayList;
import java.util.List;


public class Lexer {
    private static Lexer instance = new Lexer();
    private List<Token> tokens = new ArrayList<>();   // 记录所有的Token

    private int line = 1;                             // 当前行号
    private String content;                           // 输入的源代码
    private int position = 0;                         // 当前读取到的字符位置


    public static Lexer getInstance() {
        return instance;
    }

    // 主分析函数
    public void analyze(String content) {
        this.content = content;
        while (position < content.length()) {
            char currentChar = content.charAt(position);

            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                }
                position++;
            } else if (Character.isLetter(currentChar) || currentChar == '_') {
                analyzeIdentifierOrKeyword();
            } else if (Character.isDigit(currentChar)) {
                analyzeNumber();
            } else if (currentChar == '"') {
                analyzeString();
            } else if (currentChar == '\'') {
                analyzeChar();
            } else if (isSingleCharDelimiter(currentChar)) {
                analyzeSingleCharDelimiter();
            } else if (currentChar == '/') {
                analyzeCommentOrDivide();
            } else if (currentChar == '&' || currentChar == '|') {
                analyzeLogicalOperators();
            } else {
//                // 遇到无法识别的符号
//                ErrorHandler.getInstance().reportError(line, "Unknown symbol: " + currentChar);
                position++;
            }
        }

        saveResults(); // 保存Token和错误到文件
    }

    private void analyzeIdentifierOrKeyword() {
        StringBuilder tokenBuilder = new StringBuilder();
        char currentChar;
        while (position < content.length() && (Character.isLetterOrDigit(currentChar = content.charAt(position)) || currentChar == '_')) {
            tokenBuilder.append(currentChar);
            position++;
        }
        String tokenStr = tokenBuilder.toString();
        Token token = TokenFactory.getInstance().createToken(tokenStr, line);
        tokens.add(token);
    }

    private void analyzeNumber() {
        StringBuilder numberBuilder = new StringBuilder();
        char currentChar;
        while (position < content.length() && Character.isDigit(currentChar = content.charAt(position))) {
            numberBuilder.append(currentChar);
            position++;
        }
        String numberStr = numberBuilder.toString();
        tokens.add(TokenFactory.getInstance().createToken(numberStr, line, TokenType.INTCON));
    }

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
            tokens.add(TokenFactory.getInstance().createToken(stringBuilder.toString(), line, TokenType.STRCON));
        }
//        else {
//            ErrorHandler.getInstance().reportError(line, "Unclosed string literal");
//        }
    }

    private void analyzeChar() {
        position++; // 跳过起始的单引号
        if (position < content.length()) {
            StringBuilder charBuilder = new StringBuilder();
            charBuilder.append("'");
            char charContent = content.charAt(position);

            if (charContent == '\\') {
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
                tokens.add(TokenFactory.getInstance().createToken(charBuilder.toString(), line, TokenType.CHRCON));
            }
//            else {
//                ErrorHandler.getInstance().reportError(line, "Unclosed character literal");
//            }
        }
    }



    private void analyzeSingleCharDelimiter() {
        StringBuilder stringBuilder = new StringBuilder();
        char currentChar = content.charAt(position);
        position++;
        if(position < content.length() && "!<>=".indexOf(currentChar) >= 0) {
                char nextChar = content.charAt(position);
                if(nextChar == '=') {
                    stringBuilder.append(currentChar).append(nextChar);
                    tokens.add(TokenFactory.getInstance().createToken(stringBuilder.toString(), line));
                    position++;
                }else {
                    tokens.add(TokenFactory.getInstance().createToken(String.valueOf(currentChar), line));
                }
        }
        else {
            tokens.add(TokenFactory.getInstance().createToken(String.valueOf(currentChar), line));
        }

    }

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
//                if (!foundEnd) {
//                    ErrorHandler.getInstance().reportError(line, "Unclosed multi-line comment");
//                }
            } else {
                // 不是注释，而是除号
                tokens.add(TokenFactory.getInstance().createToken("/", line, TokenType.DIV));
            }
        }
    }



    private void analyzeLogicalOperators() {
        char currentChar = content.charAt(position);
        position++;
        if (position < content.length() && content.charAt(position) == currentChar) {
            if (currentChar == '&') {
                tokens.add(TokenFactory.getInstance().createToken("&&", line, TokenType.AND));
            } else if (currentChar == '|') {
                tokens.add(TokenFactory.getInstance().createToken("||", line, TokenType.OR));
            }
            position++;
        } else {
            // 处理非法符号 & 或 |
            ErrorHandler.getInstance().reportError(line, ErrorType.ILLEGAL_SYMBOL);
        }
    }

    private boolean isSingleCharDelimiter(char c) {
        return "+-*%(){}[],;!<>=".indexOf(c) >= 0;
    }

    private void saveResults() {
        IOUtils.writeTokensToFile(tokens, "lexer.txt");
        IOUtils.writeErrorsToFile(ErrorHandler.getInstance().getErrors(), "error.txt");
    }
}
