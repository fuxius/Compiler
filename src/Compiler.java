import frontEnd.Lexer;
import frontEnd.Parser;
import token.TokenManager;
import error.ErrorHandler;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) {
        try {
            // 读取源代码文件
            String content = new String(Files.readAllBytes(Paths.get("testfile.txt")));
            // 获取词法分析器实例
            Lexer lexer = Lexer.getInstance();
            // 进行词法分析
            lexer.analyze(content);
            // 获取解析器实例
            Parser parser = Parser.getInstance();
            // 进行语法分析
            parser.parseCompUnit();
            // 输出错误信息到 error.txt
            ErrorHandler.getInstance().outputErrors();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
