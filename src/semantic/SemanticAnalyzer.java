package semantic;

import ast.*;
import error.ErrorHandler;
import error.ErrorType;
import symbol.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义分析器，遍历 AST，进行语义检查和符号表构建。
 */
public class SemanticAnalyzer {
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;
    private int loopCount = 0; // 循环嵌套计数器
    private String currentFunctionReturnType = null; // 当前函数的返回类型
    private boolean hasReturnStatement = false; // 当前函数是否有 return 语句
    private boolean hasSemanticError = false; // 是否存在语义错误

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errorHandler = ErrorHandler.getInstance();
    }

}
