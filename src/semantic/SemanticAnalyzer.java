package semantic;

import ast.*;
import error.ErrorHandler;
import error.ErrorType;
import symbol.*;
import token.Token;
import token.TokenType;
import util.IOUtils;

import javax.lang.model.type.NullType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 语义分析器，遍历AST，建立符号表并检测语义错误
 * 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
 */
public class SemanticAnalyzer {
    private SymbolTable symbolTable; // 符号表
    private ErrorHandler errorHandler; // 错误处理器
    private int scopeLevel = 1; // 当前作用域层级，初始为全局作用域1
    private int loopCount = 0; // 循环嵌套层数，用于检测错误类型'm'

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errorHandler = ErrorHandler.getInstance();
    }

    /**
     * 分析编译单元
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    public void analyze(CompUnitNode compUnitNode) {
        // 进入全局作用域
        traverseCompUnit(compUnitNode);
        // 输出符号表信息到 symbol.txt
        outputSymbolTable();
    }

    /**
     * 遍历编译单元
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    private void traverseCompUnit(CompUnitNode compUnitNode) {
        // 遍历全局声明 Decl → ConstDecl | VarDecl
        for (DeclNode declNode : compUnitNode.getDeclNodes()) {
            traverseDecl(declNode);
        }
        // 遍历函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        for (FuncDefNode funcDefNode : compUnitNode.getFuncDefNodes()) {
            traverseFuncDef(funcDefNode);
        }
        // 处理主函数 MainFuncDef → 'int' 'main' '(' ')' Block
        traverseMainFuncDef(compUnitNode.getMainFuncDefNode());
    }

    /**
     * 遍历声明节点
     * Decl → ConstDecl | VarDecl
     */
    private void traverseDecl(DeclNode declNode) {
        if (declNode.getConstDeclNode() != null) {
            traverseConstDecl(declNode.getConstDeclNode());
        } else if (declNode.getVarDeclNode() != null) {
            traverseVarDecl(declNode.getVarDeclNode());
        }
    }

    /**
     * 遍历常量声明
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     */
    private void traverseConstDecl(ConstDeclNode constDeclNode) {
        for (ConstDefNode constDefNode : constDeclNode.getConstDefNodes()) {
            traverseConstDef(constDefNode, constDeclNode.getbTypeNode().getToken());
        }
    }

    /**
     * 遍历常量定义
     * ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
     */
    private void traverseConstDef(ConstDefNode constDefNode, Token bTypeToken) {
        String name = constDefNode.getToken().getValue();
        int lineNumber = constDefNode.getToken().getLine();
        int dimension = constDefNode.getConstExpNode() == null ? 0 : 1; // 数组维度
        boolean isConst = true;

        // 检查名称重定义（错误类型 'b'）
        if (symbolTable.lookupInCurrentScope(name) != null) {
            errorHandler.reportError(lineNumber, ErrorType.REDEFINED_IDENT);
        } else {
            // 确定类型名称
            String typeName = getTypeName(bTypeToken, isConst, dimension);
            VariableSymbol symbol = new VariableSymbol(name, symbolTable.getCurrentScopeLevel(), typeName, isConst, dimension);
            symbolTable.addSymbol(symbol);
        }

        // 处理数组维度中的常量表达式 ConstExp → AddExp
        if(constDefNode.getConstExpNode()!=null) {
            traverseConstExp(constDefNode.getConstExpNode());
        }

        // 处理常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
        traverseConstInitVal(constDefNode.getConstInitValNode());
    }

    /**
     * 遍历常量初值
     * ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
     */
    private void traverseConstInitVal(ConstInitValNode constInitValNode) {
        if (constInitValNode.getConstExpNode() != null) {
            // 情况1：单个常量表达式
            traverseConstExp(constInitValNode.getConstExpNode());
        } else if (constInitValNode.getConstExpNodeList() != null) {
            // 情况2：常量表达式列表（数组初始化）
            for (ConstExpNode constExpNode : constInitValNode.getConstExpNodeList()) {
                traverseConstExp(constExpNode);
            }
        }
    }

    /**
     * 遍历变量声明
     * VarDecl → BType VarDef { ',' VarDef } ';'
     */
    private void traverseVarDecl(VarDeclNode varDeclNode) {
        for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
            traverseVarDef(varDefNode, varDeclNode.getbTypeNode().getToken());
        }
    }

    /**
     * 遍历变量定义
     * VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
     */
    private void traverseVarDef(VarDefNode varDefNode, Token bTypeToken) {
        String name = varDefNode.getIdent();
        int lineNumber = varDefNode.getToken().getLine();
        int dimension = varDefNode.getConstExpNode() == null ? 0 : 1; // 数组维度（0 或 1）
        boolean isConst = false;

        // 检查名称重定义（错误类型 'b'）
        if (symbolTable.lookupInCurrentScope(name) != null) {
            errorHandler.reportError(lineNumber, ErrorType.REDEFINED_IDENT);
        } else {
            // 确定类型名称
            String typeName = getTypeName(bTypeToken, isConst, dimension);
            VariableSymbol symbol = new VariableSymbol(name, symbolTable.getCurrentScopeLevel(), typeName, isConst, dimension);
            symbolTable.addSymbol(symbol);
        }

        // 处理数组维度中的常量表达式（如果有） ConstExp → AddExp
        if (varDefNode.getConstExpNode() != null) {
            traverseConstExp(varDefNode.getConstExpNode());
        }

        // 处理变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
        if (varDefNode.getInitValNode() != null) {
            traverseInitVal(varDefNode.getInitValNode());
        }
    }

    /**
     * 遍历常量表达式
     * ConstExp → AddExp
     */
    private void traverseConstExp(ConstExpNode constExpNode) {
        traverseAddExp(constExpNode.getAddExpNode());
    }

    /**
     * 遍历变量初值
     * InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
     */
    private void traverseInitVal(InitValNode initValNode) {
        if (initValNode.getExpNode() != null) {
            traverseExp(initValNode.getExpNode());
        } else if (initValNode.getExpNodeList() != null) {
            for (ExpNode subExpNodeNode : initValNode.getExpNodeList()) {
                traverseExp(subExpNodeNode);
            }
        }
    }

    /**
     * 遍历函数定义
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     */
    private void traverseFuncDef(FuncDefNode funcDefNode) {
        String name = funcDefNode.getToken().getValue();
        int lineNumber = funcDefNode.getToken().getLine();
        Token funcTypeToken = funcDefNode.getFuncTypeNode().getToken();

        // 检查名称重定义（错误类型 'b'）
        if (symbolTable.lookupInCurrentScope(name) != null) {
            errorHandler.reportError(lineNumber, ErrorType.REDEFINED_IDENT);
        } else {
            // 创建函数符号
            String typeName = getFuncTypeName(funcTypeToken);
            List<VariableSymbol> params = new ArrayList<>();
            if (funcDefNode.getFuncFParamsNode() != null) {

            }
            FunctionSymbol functionSymbol = new FunctionSymbol(name, symbolTable.getCurrentScopeLevel(), typeName, funcTypeToken.getValue(), params);
            symbolTable.addSymbol(functionSymbol);
        }

        // 进入函数作用域
        symbolTable.enterScope(); // scopeLevel + 1
        // 添加函数参数到符号表 FuncFParams → FuncFParam { ',' FuncFParam }
        if (funcDefNode.getFuncFParamsNode() != null) {
            traverseFuncFParams(funcDefNode.getFuncFParamsNode(), funcDefNode.getFuncTypeNode().getToken());
        }
        // 遍历函数体 Block → '{' { BlockItem } '}'
        traverseBlock(funcDefNode.getBlockNode(), funcTypeToken);
        // 退出函数作用域
        symbolTable.exitScope();




    }

    /**
     * 遍历主函数定义
     * MainFuncDef → 'int' 'main' '(' ')' Block
     */
    private void traverseMainFuncDef(MainFuncDefNode mainFuncDefNode) {
        String name = "main";
        String typeName = "IntFunc";

        // 添加 main 函数符号
        FunctionSymbol mainFunction = new FunctionSymbol(name, symbolTable.getCurrentScopeLevel(), typeName, "int", new ArrayList<>());
//        symbolTable.addSymbol(mainFunction);

        // 进入 main 函数作用域
        symbolTable.enterScope(); // scopeLevel + 1

        // 遍历函数体 Block → '{' { BlockItem } '}'
        traverseBlock(mainFuncDefNode.getBlockNode(), mainFuncDefNode.getToken());

        // 退出 main 函数作用域
        symbolTable.exitScope();
    }

    /**
     * 遍历函数形参列表
     * FuncFParams → FuncFParam { ',' FuncFParam }
     */
    private void traverseFuncFParams(FuncFParamsNode funcFParamsNode, Token funcTypeToken) {
        for (FuncFParamNode funcFParamNode : funcFParamsNode.getFuncFParamNodes()) {
            traverseFuncFParam(funcFParamNode);
        }
    }

    /**
     * 遍历函数形参
     * FuncFParam → BType Ident ['[' ']']
     */
    private void traverseFuncFParam(FuncFParamNode funcFParamNode) {
        String name = funcFParamNode.getToken().getValue();
        int lineNumber = funcFParamNode.getToken().getLine();
        int dimension = funcFParamNode.isArray()? 1 : 0; // 数组维度
        boolean isConst = false;
        Token bTypeToken = funcFParamNode.getbTypeNode().getToken();

        // 检查名称重定义（错误类型 'b'）
        if (symbolTable.lookupInCurrentScope(name) != null) {
            errorHandler.reportError(lineNumber, ErrorType.REDEFINED_IDENT);
        } else {
            // 确定类型名称
            String typeName = getTypeName(bTypeToken, isConst, dimension);
            VariableSymbol symbol = new VariableSymbol(name, symbolTable.getCurrentScopeLevel(), typeName, isConst, dimension);
            symbolTable.addSymbol(symbol);
        }

    }

    /**
     * 遍历代码块
     * Block → '{' { BlockItem } '}'
     */
    private void traverseBlock(BlockNode blockNode, Token funcTypeToken) {
        // 进入新的作用域
//        symbolTable.enterScope(); // scopeLevel + 1

        // 遍历代码块项 BlockItem → Decl | Stmt
        for (BlockItemNode blockItemNode : blockNode.getBlockItemNodes()) {
            traverseBlockItem(blockItemNode, funcTypeToken);
        }

        // 检查有返回值的函数是否缺少 return 语句（错误类型 'g'）
        if (isInFunction() && isFuncTypeWithReturn(funcTypeToken)) {
            // 获取代码块中的最后一个 BlockItemNode
            List<BlockItemNode> blockItems = blockNode.getBlockItemNodes();
            if (blockItems.isEmpty() ||
                    blockItems.get(blockItems.size() - 1).getStmtNode() == null ||
                    blockItems.get(blockItems.size() - 1).getStmtNode().getToken() == null) {
                // 如果没有 return 语句，获取右大括号的行号并报告错误
                int lineNumber = blockNode.getToken().getLine();
                errorHandler.reportError(lineNumber, ErrorType.MISSING_RETURN_VALUE);
            }
        }

        // 退出作用域
//        symbolTable.exitScope();
    }


    /**
     * 遍历代码块项
     * BlockItem → Decl | Stmt
     */
    private void traverseBlockItem(BlockItemNode blockItemNode, Token funcTypeToken) {
        if (blockItemNode.getDeclNode() != null) {
            traverseDecl(blockItemNode.getDeclNode());
        } else if (blockItemNode.getStmtNode() != null) {
            traverseStmt(blockItemNode.getStmtNode(), funcTypeToken);
        }
    }

    /**
     * 遍历语句
     * Stmt → LVal '=' Exp ';' | [Exp] ';' | Block | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     *       | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     *       | 'break' ';' | 'continue' ';' | 'return' [Exp] ';'
     *       | LVal '=' 'getint''('')'';' | LVal '=' 'getchar''('')'';'
     *       | 'printf''('StringConst {','Exp}')'';'
     */
    private void traverseStmt(StmtNode stmtNode, Token funcTypeToken) {
        StmtType type = stmtNode.getStmtType();
        switch (type) {
            case ASSIGN:
                // 赋值语句 Stmt → LVal '=' Exp ';'
                traverseLVal(stmtNode.getlValNode(), true); // Corrected method name
                traverseExp(stmtNode.getExpNode());
                break;

            case EXP:
                // 表达式语句 Stmt → [Exp] ';'
                if (stmtNode.getExpNode() != null) {
                    traverseExp(stmtNode.getExpNode());
                }
                break;

            case BLOCK:
                symbolTable.enterScope();
                // 代码块 Stmt → Block
                traverseBlock(stmtNode.getBlockNode(), funcTypeToken);
                symbolTable.exitScope();
                break;

            case IF:
                // if 语句 Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                traverseCond(stmtNode.getCondNode());
                traverseStmt(stmtNode.getStmtNode1(), funcTypeToken);
                if (stmtNode.getStmtNode2() != null) {
                    traverseStmt(stmtNode.getStmtNode2(), funcTypeToken);
                }
                break;

            case FOR:
                // for 语句 Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                if (stmtNode.getForStmtNode1() != null) {
                    traverseForStmt(stmtNode.getForStmtNode1());
                }
                if (stmtNode.getCondNode() != null) {
                    traverseCond(stmtNode.getCondNode());
                }
                if (stmtNode.getForStmtNode2() != null) {
                    traverseForStmt(stmtNode.getForStmtNode2());
                }
                loopCount++;
                traverseStmt(stmtNode.getStmtNode1(), funcTypeToken); // Corrected to traverse the body of the loop
                loopCount--;
                break;

            case BREAKorCONTINUE:
                // break 和 continue 语句 Stmt → 'break' ';' | 'continue' ';'
                // 检测错误类型 'm'
                if (loopCount == 0) {
                    int lineNumber = stmtNode.getToken().getLine(); // Use token for line number
                    errorHandler.reportError(lineNumber, ErrorType.BREAK_CONTINUE_OUTSIDE_LOOP);
                }
                break;

            case RETURN:
                // return 语句 Stmt → 'return' [Exp] ';'
                if (isInFunction()) {
                    if (isFuncTypeVoid(funcTypeToken) && stmtNode.getExpNode() != null) {
                        // 检测错误类型 'f'
                        int lineNumber = stmtNode.getToken().getLine(); // Use token for line number
                        errorHandler.reportError(lineNumber, ErrorType.FUNC_RETURN_MISMATCH);
                    }
                }
                if (stmtNode.getExpNode() != null) {
                    traverseExp(stmtNode.getExpNode());
                }
                break;

            case GET:
                // 读取整数或字符 Stmt → LVal '=' 'getint' '(' ')' ';' | LVal '=' 'getchar' '(' ')' ';'
                traverseLVal(stmtNode.getlValNode(), true); // Corrected method name
                break;

            case PRINTF:
                // printf 语句 Stmt → 'printf' '(' StringConst { ',' Exp } ')' ';'
                int formatCount = countFormatSpecifiers(stmtNode.getStringConst());
                int expCount = stmtNode.getExpNodeList()!= null?stmtNode.getExpNodeList().size():0;
                if (formatCount != expCount) {
                    // 检测错误类型 'l'
                    int lineNumber = stmtNode.getToken().getLine(); // Use token for line number
                    errorHandler.reportError(lineNumber, ErrorType.PRINTF_ARG_MISMATCH);
                }
                for (ExpNode expNode : stmtNode.getExpNodeList()) {
                    traverseExp(expNode);
                }
                break;

            default:
                // 其他情况，根据需要添加
                break;
        }
    }
    /**
     * 遍历 ForStmt
     * ForStmt → LVal '=' Exp
     */
    private void traverseForStmt(ForStmtNode forStmtNode) {
        if (forStmtNode.getlValNode() != null && forStmtNode.getExpNode() != null) {
            // 遍历赋值的左值
            traverseLVal(forStmtNode.getlValNode(), true);
            // 遍历赋值的表达式
            traverseExp(forStmtNode.getExpNode());
        }
    }

    /**
     * 遍历表达式
     * Exp → AddExp
     */
    private void traverseExp(ExpNode expNode) {
        traverseAddExp(expNode.getAddExpNode());
    }

    /**
     * 遍历条件表达式
     * Cond → LOrExp
     */
    private void traverseCond(CondNode condNode) {
        traverseLOrExp(condNode.getlOrExpNode());
    }

    /**
     * 遍历 LVal
     * LVal → Ident ['[' Exp ']']
     */
    private void traverseLVal(LValNode lValNode, boolean isAssignment) {
        String name = lValNode.getToken().getValue();
        int lineNumber = lValNode.getToken().getLine();

        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            // 检测错误类型 'c'
            errorHandler.reportError(lineNumber, ErrorType.UNDEFINED_IDENT);
        } else if (symbol instanceof VariableSymbol) {
            VariableSymbol variableSymbol = (VariableSymbol) symbol;
            if (isAssignment && variableSymbol.isConst()) {
                // 检测错误类型 'h'
                errorHandler.reportError(lineNumber, ErrorType.MODIFY_CONST);
            }
        }

        // 处理数组下标表达式 Exp
        if(lValNode.getExpNode() != null) {
            traverseExp(lValNode.getExpNode());
        }
    }

    /**
     * 遍历加减表达式
     * AddExp → MulExp | AddExp ('+' | '−') MulExp
     */
    private void traverseAddExp(AddExpNode addExpNode) {
        if(addExpNode.getSingleMulExpNode()!=null){
            traverseMulExp(addExpNode.getSingleMulExpNode());
        }else {
            traverseMulExp(addExpNode.getMulExpNode());
            if (addExpNode.getAddExpNode() != null) {
                traverseAddExp(addExpNode.getAddExpNode());
            }
        }
    }

    /**
     * 遍历乘除模表达式
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     */
    private void traverseMulExp(MulExpNode mulExpNode) {
        if(mulExpNode.getSingleUnaryExpNode()!=null){
            traverseUnaryExp(mulExpNode.getSingleUnaryExpNode());
        }else {
            traverseUnaryExp(mulExpNode.getUnaryExpNode());
            if (mulExpNode.getMulExpNode() != null) {
                traverseMulExp(mulExpNode.getMulExpNode());
            }
        }
    }

    /**
     * 遍历一元表达式
     * UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     */
    private void traverseUnaryExp(UnaryExpNode unaryExpNode) {
        if (unaryExpNode.getPrimaryExpNode() != null) {
            traversePrimaryExp(unaryExpNode.getPrimaryExpNode());
        } else if (unaryExpNode.getUnaryExpNode() != null) {
            traverseUnaryExp(unaryExpNode.getUnaryExpNode());
        } else if (unaryExpNode.getToken() != null) {
            // 函数调用 UnaryExp → Ident '(' [FuncRParams] ')'
            String name = unaryExpNode.getToken().getValue();
            int lineNumber = unaryExpNode.getToken().getLine();

            Symbol symbol = symbolTable.lookup(name);
            if (symbol == null) {
                // 检测错误类型 'c'
                errorHandler.reportError(lineNumber, ErrorType.UNDEFINED_IDENT);
            } else if (symbol instanceof FunctionSymbol) {
                FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
                int expectedParamCount = functionSymbol.getParameters() != null? functionSymbol.getParameters().size() : 0;
                int actualParamCount = 0;
                if (unaryExpNode.getFuncRParamsNode() != null) {
                    actualParamCount = unaryExpNode.getFuncRParamsNode().getExpNodes()!= null? unaryExpNode.getFuncRParamsNode().getExpNodes().size():0;
                    traverseFuncRParams(unaryExpNode.getFuncRParamsNode());
                }
                // 检测函数参数个数不匹配（错误类型 'd'）
                if (expectedParamCount != actualParamCount) {
                    errorHandler.reportError(lineNumber, ErrorType.FUNC_PARAM_COUNT_MISMATCH);
                } else {
                    // 检测函数参数类型不匹配（错误类型 'e'）
                    // 由于类型系统复杂，简化处理或根据需要实现
                }
            }
        }
    }

    /**
     * 遍历基本表达式
     * PrimaryExp → '(' Exp ')' | LVal | Number | Character
     */
    private void traversePrimaryExp(PrimaryExpNode primaryExpNode) {
        if (primaryExpNode.getExpNode() != null) {
            traverseExp(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getlValNode() != null) {
            traverseLVal(primaryExpNode.getlValNode(), false);
        } else {
            // 数字或字符常量，不需要处理
        }
    }

    /**
     * 遍历函数实参列表
     * FuncRParams → Exp { ',' Exp }
     */
    private void traverseFuncRParams(FuncRParamsNode funcRParamsNode) {
        for (ExpNode expNode : funcRParamsNode.getExpNodes()) {
            traverseExp(expNode);
        }
    }

    /**
     * 遍历逻辑或表达式
     * LOrExp → LAndExp | LOrExp '||' LAndExp
     */
    private void traverseLOrExp(LOrExpNode lOrExpNode) {
        if(lOrExpNode.getSingleLAndExpNode()!= null){
            traverseLAndExp(lOrExpNode.getSingleLAndExpNode());
        }else {
            traverseLAndExp(lOrExpNode.getlAndExpNode());
            if (lOrExpNode.getlOrExpNode() != null) {
                traverseLOrExp(lOrExpNode.getlOrExpNode());
            }
        }
    }

    /**
     * 遍历逻辑与表达式
     * LAndExp → EqExp | LAndExp '&&' EqExp
     */
    private void traverseLAndExp(LAndExpNode lAndExpNode) {
        if(lAndExpNode.getSingleEqExpNode() != null){
            traverseEqExp(lAndExpNode.getSingleEqExpNode());
        }else {
            traverseEqExp(lAndExpNode.getSingleEqExpNode());
            if (lAndExpNode.getlAndExpNode() != null) {
                traverseLAndExp(lAndExpNode.getlAndExpNode());
            }
        }
    }

    /**
     * 遍历相等性表达式
     * EqExp → RelExp | EqExp ('==' | '!=') RelExp
     */
    private void traverseEqExp(EqExpNode eqExpNode) {
        if (eqExpNode.getSingleRelExpNode() != null) {
            traverseRelExp(eqExpNode.getSingleRelExpNode());

        }else {
            traverseRelExp(eqExpNode.getRelExpNode());
            if (eqExpNode.getEqExpNode() != null) {
                traverseEqExp(eqExpNode.getEqExpNode());
            }
        }
    }

    /**
     * 遍历关系表达式
     * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     */
    private void traverseRelExp(RelExpNode relExpNode) {
        if(relExpNode.getSingleAddExpNode() != null){
            traverseAddExp(relExpNode.getSingleAddExpNode());
        }else {
            traverseAddExp(relExpNode.getAddExpNode());
            if (relExpNode.getRelExpNode() != null) {
                traverseRelExp(relExpNode.getRelExpNode());
            }
        }
    }

    /**
     * 判断是否在函数内部
     */
    private boolean isInFunction() {
        // 当前作用域是否为函数作用域
        return symbolTable.isCurrentScopeFunction();
    }

    /**
     * 判断函数是否为 void 类型
     * FuncType → 'void' | 'int' | 'char'
     */
    private boolean isFuncTypeVoid(Token funcTypeToken) {
        return funcTypeToken.getType() == TokenType.VOIDTK;
    }

    /**
     * 判断函数是否需要返回值
     * FuncType → 'int' | 'char'
     */
    private boolean isFuncTypeWithReturn(Token funcTypeToken) {
        return funcTypeToken.getType() == TokenType.INTTK || funcTypeToken.getType() == TokenType.CHARTK;
    }


    /**
     * 统计格式化字符串中的 %d 个数
     */
    private int countFormatSpecifiers(String formatString) {
        int count = 0;
        for (int i = 0; i < formatString.length() - 1; i++) {
            if (formatString.charAt(i) == '%' && formatString.charAt(i + 1) == 'd') {
                count++;
                i++; // 跳过 'd'
            }
        }
        return count;
    }

    /**
     * 确定类型名称
     * BType → 'int' | 'char'
     */
    private String getTypeName(Token bTypeToken, boolean isConst, int dimension) {
        String baseType = bTypeToken.getType() == TokenType.INTTK ? "Int" : "Char";
        if (isConst) {
            baseType = "Const" + baseType;
        }
        if (dimension == 0) {
            return baseType;
        } else if (dimension >= 1) {
            return baseType + "Array";
        }
        return baseType;
    }

    /**
     * 确定函数类型名称
     * FuncType → 'void' | 'int' | 'char'
     */
    private String getFuncTypeName(Token funcTypeToken) {
        if (funcTypeToken.getType() == TokenType.VOIDTK) {
            return "VoidFunc";
        } else if (funcTypeToken.getType() == TokenType.INTTK) {
            return "IntFunc";
        } else if (funcTypeToken.getType() == TokenType.CHARTK) {
            return "CharFunc";
        }
        return "UnknownFunc";
    }

    /**
     * 输出符号表信息到 symbol.txt
     */
    private void outputSymbolTable() {
        // 获取符号表中的所有符号
        List<Symbol> symbols = symbolTable.getAllSymbols();

        // 按照 getScopeLevel 进行升序排序
        symbols.sort(Comparator.comparingInt(Symbol::getScopeLevel));

        // 创建一个列表来存储所有输出的内容
        List<String> outputLines = new ArrayList<>();

        // 生成每一行输出并添加到列表
        for (Symbol symbol : symbols) {
            String output = symbol.getScopeLevel() + " " + symbol.getName() + " " + symbol.getTypeName();
            outputLines.add(output);
        }

        // 将所有行一次性写入 symbol.txt 文件
        IOUtils.writeToFile("symbol.txt", outputLines);
    }
}