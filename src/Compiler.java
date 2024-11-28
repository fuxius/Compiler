import LLVMIR.IRBuilder;
import ast.CompUnitNode;
import backEnd.MipsBuilder;
import frontEnd.Lexer;
import frontEnd.Parser;
//import frontEnd.SemanticAnalyzer;
import frontEnd.SemanticAnalyzer;
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
            // 进行语法分析，生成 AST
            CompUnitNode compUnitNode = parser.parseCompUnit();
            //进行语义分析
            SemanticAnalyzer semanticAnalyzer = SemanticAnalyzer.getInstance();
            semanticAnalyzer.analyze(compUnitNode);
            // 输出错误信息到 error.txt
            ErrorHandler.getInstance().outputErrors();
            // 构建并生成main函数
            IRBuilder irBuilder = IRBuilder.getInstance();
            irBuilder.analyze(compUnitNode);
            irBuilder.outputLLVMIRToFile();
            // 构建并生成 MIPS 汇编代码
            MipsBuilder mipsBuilder = MipsBuilder.getInstance();
            mipsBuilder.mipsBuilder(irBuilder.getModule());


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
