package token;

import java.util.ArrayList;
import java.util.List;

public class TokenManager {
    private static final TokenManager instance = new TokenManager();

    public static TokenManager getInstance() {
        return instance;
    }
    private List<Token> tokens = new ArrayList<>(); // 保存所有的Token
    private int currentPosition = 0;                 // 当前Token的位置

    // 保存一个Token到列表中
    public void saveToken(Token token) {
        tokens.add(token);
    }

    public List<Token> getTokens() {
        return tokens;
    }

    // 获取当前的Token
    public Token getToken() {
        if (currentPosition < tokens.size()) {
            return tokens.get(currentPosition);
        }
        return null; // 表示没有更多的Token了
    }

    // 读取下一个Token
    public void nextToken() {
        if (currentPosition < tokens.size() - 1) {
            currentPosition++;
        }
    }

    // 超前查看第 pos 个Token，不改变当前Token状态
    public Token lookAhead(int offset) {
        int targetIndex = currentPosition + offset;
        if (targetIndex >= tokens.size()) {
            return new Token(TokenType.EOF, "",-1); // 假设有 EOF 类型
        }
        return tokens.get(targetIndex);
    }

    // 获取当前Token的位置
    public int getCurrentPosition() {
        return currentPosition;
    }

    // 回退到指定的位置
    public void rewindToIndex(int index) {
        if (index >= 0 && index < tokens.size()) {
            currentPosition = index;
        }
    }

    public int getTotalTokens() {
        return tokens.size();
    }


}
