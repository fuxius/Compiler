package LLVMIR;

import LLVMIR.Base.*;
import LLVMIR.Base.Module;
import LLVMIR.Global.ConstStr;
import LLVMIR.Global.GlobalVar;
import LLVMIR.Ins.*;
import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import ast.*;
import symbol.FunctionSymbol;
import symbol.SymbolTable;
import symbol.VariableSymbol;
import token.Token;
import LLVMIR.Global.Function;
import token.TokenType;

import java.util.*;

/**
 * 中间代码生成器，遍历AST，建立符号表
 */
public class IRBuilder {
    private static final IRBuilder  instance = new IRBuilder();

    public static IRBuilder getInstance() {
        return instance;
    }
    private SymbolTable symbolTable; // 符号表
    private FunctionSymbol currentFunction = null;
    private boolean isGlobal = false;
    private int varId = 0;
    private int blockId = 0;
    private int funcId = 0;
    private int globalId = 0;
    private int strId = 0;
    private int paramId = 0;
    private Function curFunc;
    private BasicBlock curBlock;
    private Stack<Loop> loopStack = new Stack<>();
    private Module module = new Module();
    public static String blockName = "b";
    private static String globalName = "@g";
    private static String strName = "@str";
    public static String paraName = "%a";
    public static String tempName = "%t";
    private static String funcName = "@func";

    public IRBuilder() {
        this.symbolTable = SymbolTable.getInstance();
    }
    public void outputLLVMIRToFile() {
        module.writeToFile("llvm_ir.txt");
    }
    // 将 IO 函数声明添加到模块头部
    public void declareIOFunctions() {
        // 添加声明：读取一个整数
        module.addFunction(new Function("getint",LLVMType.Int32));

        // 添加声明：读取一个字符
        module.addFunction(new Function("getchar",LLVMType.Int32));

        // 添加声明：输出一个整数
        module.addFunction(new Function("putint",LLVMType.Void));
        // 添加声明：输出一个字符
        module.addFunction(new Function("putch",LLVMType.Void));
        // 添加声明：输出一个字符串
        module.addFunction(new Function("putstr",LLVMType.Void));
    }
    private void resetIds() {
        paramId = 0; // 参数编号从 0 开始
        varId = 0;
        blockId = 0;
    }
    private int getVarId() {
        return ++varId;
    }

    private int getFuncId() {
        return ++funcId;
    }

    private int getBlockId() {
        return ++blockId;
    }

    private int getGlobalId() {
        return ++globalId;
    }

    private int getStrId() {
        return ++strId;
    }

    private int getParamId() {
        return ++paramId;
    }

    public Module getModule() {
        return module;
    }



    /**
     * 分析编译单元
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    public void analyze(CompUnitNode compUnitNode) {
        // 进入全局作用域
        buildCompUnit(compUnitNode);
    }

    /**
     * 遍历编译单元
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    private void buildCompUnit(CompUnitNode compUnitNode) {
        isGlobal = true;
        // 遍历全局声明 Decl → ConstDecl | VarDecl
        for (DeclNode declNode : compUnitNode.getDeclNodes()) {
            buildDecl(declNode);
        }
        isGlobal = false;
        // 遍历函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        for (FuncDefNode funcDefNode : compUnitNode.getFuncDefNodes()) {
            buildFuncDef(funcDefNode);
        }
        // 处理主函数 MainFuncDef → 'int' 'main' '(' ')' Block
        buildMainFuncDef(compUnitNode.getMainFuncDefNode());
    }

    /**
     * 遍历声明节点
     * Decl → ConstDecl | VarDecl
     */
    private void buildDecl(DeclNode declNode) {
        if (declNode.getConstDeclNode() != null) {
            buildConstDecl(declNode.getConstDeclNode());
        } else if (declNode.getVarDeclNode() != null) {
            buildVarDecl(declNode.getVarDeclNode());
        }
    }

    /**
     * 遍历常量声明
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     */
    private void buildConstDecl(ConstDeclNode constDeclNode) {
        for (ConstDefNode constDefNode : constDeclNode.getConstDefNodes()) {
            buildConstDef(constDefNode, constDeclNode.getbTypeNode().getToken());
        }
    }


    /**
     * 生成 LLVM IR 的常量定义
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     */
    private void buildConstDef(ConstDefNode constDefNode, Token token) {
        constDefNode.initializeVariableSymbolForLLVM();
        // 获取常量符号信息（在语义分析阶段已设置好符号信息）
        VariableSymbol constSymbol = constDefNode.getVariableSymbol();

        if (isGlobal) {
            // 全局常量
            GlobalVar globalVar = new GlobalVar(
                    globalName + getGlobalId(),
                    new PointerType(constSymbol.getLLVMType()), // 指针类型
                    constSymbol.getInitialValues(),             // 初始值
                    false,                                      // 非零初始化
                    constSymbol.getLength(),                 // 数组长度
                    true                                        // 标记为常量
            );
            module.addGlobalVar(globalVar); // 添加到模块
            constSymbol.setLLVMIR(globalVar); // 关联 LLVM 对象
        } else {
            // 局部常量
            Alloca allocaInstr;
            if (constSymbol.getDimension() == 0) {
                // 非数组常量
                allocaInstr = new Alloca(tempName + getVarId(), curBlock, constSymbol.getLLVMType());
            } else {
                // 数组常量
                allocaInstr = new Alloca(tempName + getVarId(), curBlock, constSymbol.getLLVMType(), constSymbol.getInitialValues());
            }
            curBlock.addInstr(allocaInstr); // 分配内存

            // 初始化常量值
            initializeConstant(constSymbol, allocaInstr);

            constSymbol.setLLVMIR(allocaInstr); // 关联 LLVM 对象
        }
    }


    /**
     * 初始化常量值
     * @param constSymbol 常量符号
     * @param allocaInstr 分配的内存指令
     */
    private void initializeConstant(VariableSymbol constSymbol, Alloca allocaInstr) {
        if (constSymbol.getDimension() == 0) {
            // 非数组常量
            initializeScalarConstant(constSymbol, allocaInstr);
        } else {
            // 数组常量
            initializeArrayConstant(constSymbol, allocaInstr);
        }
    }

    /**
     * 初始化标量常量值
     * @param constSymbol 常量符号
     * @param allocaInstr 分配的内存指令
     */
    private void initializeScalarConstant(VariableSymbol constSymbol, Alloca allocaInstr) {
        // 获取初始值
        Value initialValue = new Constant(constSymbol.getInitialValues().get(0));
        // 进行类型转换
        Value convertedValue = convertType(initialValue, constSymbol.getLLVMType());
        // 创建存储指令
        Store storeInstr = new Store(convertedValue, allocaInstr, curBlock);
        curBlock.addInstr(storeInstr);
    }

    /**
     * 初始化数组常量值
     * @param constSymbol 常量符号
     * @param allocaInstr 分配的内存指令
     */
    private void initializeArrayConstant(VariableSymbol constSymbol, Alloca allocaInstr) {
        ArrayList<Integer> initialValues = constSymbol.getInitialValues();
        int length = constSymbol.getLength();

        for (int i = 0; i < length; i++) {
            // 获取数组元素的指针
            Value index = new Constant(i);
            GetPtr getPtrInstr = new GetPtr(tempName + getVarId(), allocaInstr, index, curBlock);
            curBlock.addInstr(getPtrInstr);

            // 获取初始值（如果不足，则补零）
            int value = (i < initialValues.size()) ? initialValues.get(i) : 0;
            Value initialValue = new Constant(value);
            ArrayType arrayType = (ArrayType)constSymbol.getLLVMType();
            // 进行类型转换
            Value convertedValue = convertType(initialValue, arrayType.getElementType());
            // 创建存储指令
            Store storeInstr = new Store(convertedValue, getPtrInstr, curBlock);
            curBlock.addInstr(storeInstr);
        }
    }



    /**
     * 遍历变量声明
     * VarDecl → BType VarDef { ',' VarDef } ';'
     */
    private void buildVarDecl(VarDeclNode varDeclNode) {
        for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
            buildVarDef(varDefNode, varDeclNode.getbTypeNode().getToken());
        }
    }


    /**
     * 生成 LLVM IR 的变量定义
     * VarDef → Ident { '[' ConstExp ']' } [ '=' InitVal ]
     */
    private void buildVarDef(VarDefNode varDefNode, Token bTypeToken) {
        varDefNode.initializeVariableSymbolForLLVM();
        // 获取并检查变量符号信息（之前语义分析阶段已设置好符号信息）
        VariableSymbol varSymbol = varDefNode.getVariableSymbol();

        // 判断是否为全局变量
        if (isGlobal) {
            // 创建全局变量
            GlobalVar globalVar = new GlobalVar(
                    globalName + getGlobalId(),
                    new PointerType(varSymbol.getLLVMType()), // 指针类型
                    varSymbol.getInitialValues(),             // 初始值
                    varSymbol.isZeroInitialized(),            // 是否初始化为零
                    varSymbol.getLength(),                   // 数组长度
                    varSymbol.isConst()                         // 非常量（变量）
            );
            module.addGlobalVar(globalVar); // 将全局变量添加到模块
            varSymbol.setLLVMIR(globalVar); // 将 LLVM 对象关联到符号表项
        } else {
            InitValNode initValNode = varDefNode.getInitValNode();
            // 处理局部变量
            // 分配内存

            // 初始化
            if (varSymbol.getDimension() == 0) {
                Alloca allocaInstr = new Alloca(tempName + getVarId(), curBlock, varSymbol.getLLVMType());
                curBlock.addInstr(allocaInstr); // 分配内存空间
                varSymbol.setLLVMIR(allocaInstr); // 将 LLVM 对象关联到符号表项
                if(initValNode != null) {
                    // 标量变量
                    ArrayList<Value> values = buildInitVal(initValNode);
                    Value convertedValue = convertType(values.get(0), varSymbol.getLLVMType());
                    Store storeInstr = new Store(convertedValue, allocaInstr, curBlock);
                    curBlock.addInstr(storeInstr); // 存储初始值
                }
            } else { // 一维数组
                int length = varSymbol.getLength();
                Alloca allocaInstr = new Alloca(tempName + getVarId(), curBlock, varSymbol.getLLVMType());
                curBlock.addInstr(allocaInstr); // 分配内存空间

                if (length > 0 && initValNode != null) {
                    // 初始化数组变量
                    ArrayList<Value> values = buildInitVal(initValNode);
                    for (int i = 0; i < length; i++) {
                        Value index = new Constant(i);
                        GetPtr getPtrInstr = new GetPtr(tempName + getVarId(), allocaInstr, index, curBlock);
                        curBlock.addInstr(getPtrInstr);

                        Value value = (i < values.size()) ? values.get(i) : new Constant(0);
                        ArrayType arrayType = (ArrayType)varSymbol.getLLVMType();
                        // 进行类型转换
                        Value convertedValue = convertType(value, arrayType.getElementType());
                        // 创建存储指令
                        Store storeInstr = new Store(convertedValue, getPtrInstr, curBlock);
                        curBlock.addInstr(storeInstr);
                    }
                }
                varSymbol.setLLVMIR(allocaInstr); // 在初始化完成后关联
            }


        }
    }

    /**
     * 遍历变量初值
     * InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
     */
    private ArrayList<Value> buildInitVal(InitValNode initValNode) {
        ArrayList<Value> returnInstrs = new ArrayList<>();

        if (initValNode.getExpNode() != null) {
            // 处理单一表达式初值
            returnInstrs.add(buildExp(initValNode.getExpNode()));
        } else if (initValNode.getExpNodeList() != null) {
            // 处理表达式列表初值
            for (ExpNode subExpNode : initValNode.getExpNodeList()) {
                returnInstrs.add(buildExp(subExpNode));
            }
        } else if (initValNode.getStringConst() != null) {
            // 处理字符串常量
            String str = initValNode.getStringConst();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                // 创建 i8 常量，每个字符对应一个 i8 值
                Constant charConst = new Constant((int) c, LLVMType.Int8);
                returnInstrs.add(charConst);
            }
            // 可选：添加 null 终止符 '\0'
            Constant nullTerminator = new Constant(0, LLVMType.Int8);
            returnInstrs.add(nullTerminator);
        }

        return returnInstrs;
    }


    /**
     * 生成 LLVM IR 的函数定义
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     */
    private void buildFuncDef(FuncDefNode funcDefNode) {
        // 获取函数符号并设置符号表
        FunctionSymbol functionSymbol = funcDefNode.getFunctionSymbol();
        String returnType = functionSymbol.getReturnType();
        // 函数参数处理
        symbolTable.enterScopeForLLVM();
        resetIds();
        // 确定返回类型
        LLVMType retType = switch (returnType) {
            case "int" -> LLVMType.Int32;
            case "char" -> LLVMType.Int8;
            case "void" -> LLVMType.Void;
            default -> throw new IllegalArgumentException("Unsupported return type: " + returnType);
        };

        // 创建函数对象
        curFunc = new Function(funcName+getFuncId(), retType);
        module.addFunction(curFunc); // 添加到模块
        functionSymbol.setLLVMIR(curFunc); // 关联符号表

        // 函数入口基本块
        curBlock = new BasicBlock(blockName + getBlockId(), curFunc);
        curFunc.addBasicBlock(curBlock);
        if (funcDefNode.getFuncFParamsNode() != null) {
            buildFuncFParams(funcDefNode.getFuncFParamsNode());
        }

        // 生成函数体
        buildBlock(funcDefNode.getBlockNode(), funcDefNode.getFuncTypeNode().getToken(), true);

        // 确保函数返回指令
        if (!curBlock.hasRet()) {
            curBlock.addInstr(retType == LLVMType.Void
                    ? new Ret(null, curBlock)
                    : new Ret(new Constant(0), curBlock));
        }

        // 退出函数作用域
        symbolTable.exitScopeForLLVM();
        curFunc.setVarId(varId);
        curFunc.setBlockId(blockId);
    }


    /**
     * 生成 LLVM IR 的主函数定义
     * MainFuncDef → 'int' 'main' '(' ')' Block
     */
    private void buildMainFuncDef(MainFuncDefNode mainFuncDefNode) {
        // 进入 main 函数作用域，重置 ID 计数器
        symbolTable.enterScopeForLLVM();
        resetIds();
        // 初始化 LLVM Function 对象，设置返回类型为 Int32
        curFunc = new Function("@main", LLVMType.Int32);
        module.addFunction(curFunc); // 将主函数添加到模块中

        // 创建函数的起始基本块
        curBlock = new BasicBlock(blockName + getBlockId(), curFunc);
        curFunc.addBasicBlock(curBlock);

        // 生成函数体的 LLVM IR
        buildBlock(mainFuncDefNode.getBlockNode(),mainFuncDefNode.getToken(),true);

        // 确保函数结尾有返回指令
        if (!curBlock.hasRet()) {
            curBlock.addInstr(new Ret(new Constant(0), curBlock)); // 默认返回值为 0
        }

        // 退出 main 函数作用域
        symbolTable.exitScopeForLLVM();
        curFunc.setVarId(varId);
        curFunc.setBlockId(blockId);
    }


    /**
     * 遍历函数形参列表
     * FuncFParams → FuncFParam { ',' FuncFParam }
     */
    private void buildFuncFParams(FuncFParamsNode funcFParamsNode) {
        for (FuncFParamNode funcFParamNode : funcFParamsNode.getFuncFParamNodes()) {
            buildFuncFParam(funcFParamNode);
        }
    }

    /**
     * 遍历单个函数形参
     * FuncFParam → BType Ident ['[' ']']
     */
    private void buildFuncFParam(FuncFParamNode funcFParamNode) {
        // 从符号表中查找参数信息
        String paramName = funcFParamNode.getToken().getValue();
        VariableSymbol varSymbol = funcFParamNode.getVariableSymbol();

        // 确定形参的 LLVM 类型
        LLVMType llvmType;
        if (varSymbol.getDimension() == 0) {
            if(varSymbol.isConst()){
                llvmType = funcFParamNode.getToken().getType() == TokenType.INTTK? LLVMType.Int32 : LLVMType.Int8;
            }else {
                // 标量：普通变量类型
                llvmType = varSymbol.getBaseType().equals("int") ? LLVMType.Int32 : LLVMType.Int8;
            }
        } else if (varSymbol.getDimension() == 1) {
            // 一维数组变量：指针类型
            llvmType = new PointerType(varSymbol.getBaseType().equals("int") ? LLVMType.Int32 : LLVMType.Int8);
        } else {
            throw new IllegalArgumentException("Unsupported dimension for FuncFParam");
        }

        // 创建参数（Param）并添加到当前函数
        Param param = new Param(paraName + getParamId(), llvmType, curFunc);
        curFunc.addParam(param);

        // 如果是标量，分配内存并存储参数值
        if (varSymbol.getDimension() == 0) {
            // 分配内存
            Alloca allocaInstr = new Alloca(tempName + getVarId(), curBlock, llvmType);
            curBlock.addInstr(allocaInstr);

            // 将参数值存储到分配的内存中
            Store storeInstr = new Store(param, allocaInstr, curBlock);
            curBlock.addInstr(storeInstr);

            // 更新符号表中符号的 LLVM IR 引用
            varSymbol.setLLVMIR(allocaInstr);
        } else if (varSymbol.getDimension() == 1) {
            // 一维数组：直接将参数作为指针
            varSymbol.setLLVMIR(param);
        }
    }


    /**
     * 遍历函数实参列表并完成类型转换
     * FuncRParams → Exp { ',' Exp }
     *
     * @param funcRParamsNode 函数实参节点
     * @param expectedTypes 函数形参的类型列表
     * @return 转换后的实参列表
     */
    private ArrayList<Value> buildFuncRParams(FuncRParamsNode funcRParamsNode, List<LLVMType> expectedTypes) {
        ArrayList<Value> values = new ArrayList<>();
        List<ExpNode> expNodes = funcRParamsNode.getExpNodes();

        for (int i = 0; i < expNodes.size(); i++) {
            // 生成实参
            Value argument = buildExp(expNodes.get(i));

            // 检查是否为数组传参
            if (argument.getType() instanceof PointerType) {
                PointerType pointerType = (PointerType) argument.getType();
                if (pointerType.getPointedType() instanceof ArrayType) {
                    // 如果是数组类型，需要使用 GEP 获取首元素地址
                    GetPtr getPtrInstr = new GetPtr(tempName + getVarId(), argument, new Constant(0), curBlock);
                    curBlock.addInstr(getPtrInstr);
                    argument = getPtrInstr; // 更新为指向数组首元素的普通指针
                }
            }

            // 确保类型一致
            LLVMType expectedType = expectedTypes.get(i);
            Value convertedArgument = convertType(argument, expectedType);

            values.add(convertedArgument);
        }

        return values;
    }
    /**
     * 遍历代码块
     * Block → '{' { BlockItem } '}'
     */
    private void buildBlock(BlockNode blockNode, Token funcTypeToken,boolean isOutermostBlock) {
        // 遍历代码块项 BlockItem → Decl | Stmt
        for (BlockItemNode blockItemNode : blockNode.getBlockItemNodes()) {
            buildBlockItem(blockItemNode, funcTypeToken);
        }
    }


    /**
     * 遍历代码块项
     * BlockItem → Decl | Stmt
     */
    private void buildBlockItem(BlockItemNode blockItemNode, Token funcTypeToken) {
        if (blockItemNode.getDeclNode() != null) {
            buildDecl(blockItemNode.getDeclNode());
        } else if (blockItemNode.getStmtNode() != null) {
            buildStmt(blockItemNode.getStmtNode(), funcTypeToken);
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
    private void buildStmt(StmtNode stmtNode, Token funcTypeToken) {
        StmtType type = stmtNode.getStmtType();
        switch (type) {
            case ASSIGN:
                // 赋值语句 Stmt → LVal '=' Exp ';'
                Value value1 = buildLValForAssign(stmtNode.getlValNode()); // Corrected method name
                Value value2 = buildExp(stmtNode.getExpNode());
                LLVMType targetType = ((PointerType) value1.getType()).getPointedType();
                Value convertedValue = convertType(value2, targetType);
                curBlock.addInstr(new Store(convertedValue,value1, curBlock));
                break;

            case EXP:
                // 表达式语句 Stmt → [Exp] ';'
                if (stmtNode.getExpNode() != null) {
                    buildExp(stmtNode.getExpNode());
                }
                break;

            case BLOCK:
                symbolTable.enterScopeForLLVM();
                // 代码块 Stmt → Block
                buildBlock(stmtNode.getBlockNode(), funcTypeToken,false);
                symbolTable.exitScopeForLLVM();
                break;

            case IF:
                // if 语句 Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                // Step 1: 构造目标块
                BasicBlock thenBlock = new BasicBlock(blockName + getBlockId(), curFunc); // thenBlock 的块
                BasicBlock nextBlock = new BasicBlock(blockName + getBlockId(), curFunc);  // BasicBlock3 (后续)
                curFunc.addBasicBlock(thenBlock);
                curFunc.addBasicBlock(nextBlock);

                BasicBlock elseBlock = null; // 如果有 else 语句则初始化 elseBlock
                if (stmtNode.getStmtNode2() != null) {
                    elseBlock = new BasicBlock(blockName + getBlockId(), curFunc); // Stmt2 的块
                    curFunc.addBasicBlock(elseBlock);
                }

                // Step 2: 生成条件表达式
                buildCond(stmtNode.getCondNode(), thenBlock, elseBlock != null ? elseBlock : nextBlock);

                // Step 3: 构造 thenBlock 的内容
                curBlock = thenBlock; // 当前块跳转到 thenBlock
                buildStmt(stmtNode.getStmtNode1(), null); // 递归构造 if 分支语句
                if (!curBlock.hasBr()) {
                    curBlock.addInstr(new Branch(nextBlock, curBlock)); // 结束后跳转到后续块
                }

                // Step 4: 构造 elseBlock 的内容（如果有）
                if (elseBlock != null) {
                    curBlock = elseBlock; // 当前块跳转到 elseBlock
                    buildStmt(stmtNode.getStmtNode2(), null); // 递归构造 else 分支语句
                    if (!curBlock.hasBr()) {
                        curBlock.addInstr(new Branch(nextBlock, curBlock)); // 结束后跳转到后续块
                    }
                }

                // Step 5: 更新当前块为下一块
                curBlock = nextBlock; // 当前块设置为 BasicBlock3
                break;

            case FOR:
                // for 语句 Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt

                // 创建基本块：条件块、循环体块、更新块和继续块
                BasicBlock condBlock = new BasicBlock(blockName + getBlockId(), curFunc);
                BasicBlock bodyBlock = new BasicBlock(blockName + getBlockId(), curFunc);
                BasicBlock updateBlock = new BasicBlock(blockName + getBlockId(), curFunc);
                BasicBlock continueBlock = new BasicBlock(blockName + getBlockId(), curFunc);

                // 将基本块添加到函数中
                curFunc.addBasicBlock(condBlock);
                curFunc.addBasicBlock(bodyBlock);
                curFunc.addBasicBlock(updateBlock);
                curFunc.addBasicBlock(continueBlock);
                // 初始化
                if (stmtNode.getForStmtNode1() != null) {
                    buildForStmt(stmtNode.getForStmtNode1(), funcTypeToken);
                }

                curBlock.addInstr(new Branch(condBlock, curBlock));

                curBlock = condBlock;
                if (stmtNode.getCondNode() != null) {
                    // 构建条件表达式
                    buildCond(stmtNode.getCondNode(), bodyBlock, continueBlock);
                } else {
                    // 如果没有条件表达式，始终跳转到循环体块
                    curBlock.addInstr(new Branch(bodyBlock, curBlock));
                }

                // 将当前循环上下文推入堆栈
                loopStack.push(new Loop(condBlock,bodyBlock, updateBlock, continueBlock));

                // 构建循环体
                curBlock = bodyBlock;
                buildStmt(stmtNode.getStmtNode1(), funcTypeToken);
                if (!curBlock.hasBr()) {
                    curBlock.addInstr(new Branch(updateBlock, curBlock));
                }

                // 构建更新语句
                curBlock = updateBlock;
                if (stmtNode.getForStmtNode2() != null) {
                    buildForStmt(stmtNode.getForStmtNode2(), funcTypeToken);
                }
                curBlock.addInstr(new Branch(condBlock, curBlock));

                // 弹出当前循环上下文
                loopStack.pop();

                // 设置当前块为继续块
                curBlock = continueBlock;
                break;

            case BREAKorCONTINUE:
                // break 和 continue 语句 Stmt → 'break' ';' | 'continue' ';'
                if (stmtNode.getKeyword().equals("break")) {
                    // break 语句
                    if (loopStack.isEmpty()) {
                        throw new IllegalStateException("Break statement not in loop");
                    }
                    Loop loop = loopStack.peek();
                    curBlock.addInstr(new Branch(loop.getExit(), curBlock));
                } else {
                    // continue 语句
                    if (loopStack.isEmpty()) {
                        throw new IllegalStateException("Continue statement not in loop");
                    }
                    Loop loop = loopStack.peek();
                    curBlock.addInstr(new Branch(loop.getUpdate(), curBlock));
                }
                break;

            case RETURN:
                // return 语句 Stmt → 'return' [Exp] ';'
                Value value = null;
                if (stmtNode.getExpNode() != null) {
                    value = buildExp(stmtNode.getExpNode());
                }else if(curFunc.getReturnType() != LLVMType.Void){
                    value = new Constant(0);
                }
                if (value != null) {
                    value = convertType(value, curFunc.getReturnType());
                }
                Ret ret = new Ret(value,curBlock);
                curBlock.addInstr(ret);
                break;

            case GET:
                // 获取左值（LVal）
                LValNode lValNode = stmtNode.getlValNode();
                // 获取操作类型（getint 或 getchar）
                String opType = stmtNode.getKeyword();
                // 根据左值生成对应的 LLVM IR 地址
                Value point = buildLValForAssign(lValNode);
                // 创建 IR 中的返回值
                if (opType.equals("getint")) {
                    // 调用 getint 函数
                    Getint getint = new Getint(tempName+getVarId(),curBlock);
                    curBlock.addInstr(getint);
                    // 使用 Store 指令将 getint 的结果存储到左值的位置
                    Store storeInstr = new Store(getint, point, curBlock);
                    curBlock.addInstr(storeInstr);  // 将 Store 指令添加到当前基本块
                } else if (opType.equals("getchar")) {
                    // 调用 getchar 函数
                    Getchar getchar = new Getchar(tempName+getVarId(),curBlock);
                    curBlock.addInstr(getchar);
                    targetType = ((PointerType) point.getType()).getPointedType();
                    convertedValue = convertType(getchar, targetType);
                    Store storeInstr = new Store(convertedValue, point, curBlock);
                    curBlock.addInstr(storeInstr);  // 将 Store 指令添加到当前基本块
                }
                break;
            // printf 语句 Stmt → 'printf' '(' StringConst { ',' Exp } ')' ';'
            case PRINTF:
                // 提取格式字符串和表达式列表
                String stringConst = stmtNode.getStringConst();
                List<ExpNode> expList = stmtNode.getExpNodeList();

                // 创建全局字符串常量的缓存
                StringBuilder sb = new StringBuilder();
                ArrayList<Value> expressions = new ArrayList<>();
                for (ExpNode expNode : expList) {
                    expressions.add(buildExp(expNode));
                }

                // 去掉字符串两侧的引号
                stringConst = stringConst.substring(1, stringConst.length() - 1);

                // 遍历格式字符串
                for (int i = 0; i < stringConst.length(); i++) {
                    char ch = stringConst.charAt(i);
                    if (ch == '%') {
                        // 遇到占位符，将当前缓存的字符串输出
                        if (sb.length() > 0) {
                            // 创建 ConstStr 对象并添加到全局模块
                            ConstStr constStr = new ConstStr(strName + getStrId(), sb.toString());
                            module.addConstant(constStr);

                            // 使用 Putstr 输出字符串
                            Putstr putStrInstr = new Putstr(curBlock, constStr);
                            curBlock.addInstr(putStrInstr);

                            // 清空缓存
                            sb = new StringBuilder();
                        }

                        // 处理占位符类型
                        i++;
                        char specifier = stringConst.charAt(i);
                        if (specifier == 'd') {
                            // 输出整数
                            value = expressions.get(0);
                            Putint putIntInstr = new Putint(value, curBlock);
                            curBlock.addInstr(putIntInstr);
                            expressions.remove(0);
                        } else if (specifier == 'c') {
                            // 对字符进行零扩展后输出
                            value = expressions.get(0);
                            Putch putChInstr = new Putch(value, curBlock);
                            curBlock.addInstr(putChInstr);
                            expressions.remove(0);
                        }
                    } else if (ch == '\\') {
                        // 转义字符处理
                        i++;
                        char nextCh = stringConst.charAt(i);
                        switch (nextCh) {
                            case 'a': sb.append('\u0007'); break; // 警报字符
                            case 'b': sb.append('\b'); break;    // 退格
                            case 't': sb.append('\t'); break;    // 水平制表符
                            case 'n': sb.append('\n'); break;    // 换行
                            case 'v': sb.append('\u000B'); break; // 垂直制表符
                            case 'f': sb.append('\f'); break;    // 换页
                            case 'r': sb.append('\r'); break;    // 回车
                            case '\\': sb.append('\\'); break;   // 反斜杠
                            case '\'': sb.append('\''); break;   // 单引号
                            case '"': sb.append('\"'); break;    // 双引号
                            case '0': sb.append('\0'); break;    // 空字符
                            default:
                                throw new IllegalArgumentException("Unsupported escape sequence: \\" + nextCh);
                        }
                    } else {
                        // 普通字符直接添加到缓存
                        sb.append(ch);
                    }
                }

                // 输出剩余的字符串
                if (sb.length() > 0) {
                    // 创建 ConstStr 对象并添加到全局模块
                    ConstStr constStr = new ConstStr(strName + getStrId(), sb.toString());
                    module.addConstant(constStr);
                    // 使用 Putstr 输出字符串
                    Putstr putStrInstr = new Putstr(curBlock, constStr);
                    curBlock.addInstr(putStrInstr);
                }
                break;


            default:
                // 其他情况，根据需要添加
                break;
        }
    }


    public Value buildLValForAssign(LValNode lValNode) {
        // 获取左值标识符（变量名）
        String name = lValNode.getToken().getValue();
        // 从符号表中获取符号
        VariableSymbol symbol =  lValNode.getVariableSymbol();
        ArrayList<Value> values = new ArrayList<>();

        // 如果左值有下标（数组），则构建对应的偏移值
        if (lValNode.getExpNode() !=null) {
            values.add(buildExp(lValNode.getExpNode()));
        }

        // 判断左值是标量、数组或多维数组
        if (symbol.getDimension() == 0) {
            // 标量：直接返回符号的 LLVM IR
            return symbol.getLLVMIR();
        } else {
            // 一维数组：使用 GetPtr 计算地址
            GetPtr getPtrInstr = new GetPtr(tempName + getVarId(), symbol.getLLVMIR(), values.get(0), curBlock);
            curBlock.addInstr(getPtrInstr);
            return getPtrInstr;
        }
    }



    /**
     * 遍历表达式
     * Exp → AddExp
     */
    private Value buildExp(ExpNode expNode) {
        return buildAddExp(expNode.getAddExpNode());
    }




    /**
     * 遍历加减表达式
     * AddExp → MulExp | AddExp ('+' | '−') MulExp
     */
    private Value buildAddExp(AddExpNode addExpNode) {
        // 构建操作数和运算符列表
        addExpNode.populateLists();
        List<MulExpNode> mulExpNodes = addExpNode.getMulExpNodes();
        List<Token> operators = addExpNode.getOperators();

        // 初始操作数
        Value operand1 = buildMulExp(mulExpNodes.get(0));

        // 遍历后续的 MulExp 和运算符
        for (int i = 0; i < operators.size(); i++) {
            Value operand2 = buildMulExp(mulExpNodes.get(i + 1));
            Token operator = operators.get(i);

            // 根据操作符生成 ALU 指令
            Alu.OP op = (operator.getType() == TokenType.PLUS) ? Alu.OP.ADD : Alu.OP.SUB;
            Alu aluInstr = new Alu(tempName + getVarId(), operand1, operand2, op, curBlock);
            curBlock.addInstr(aluInstr);

            // 更新操作数
            operand1 = aluInstr;
        }

        return operand1;
    }


    /**
     * 遍历乘除模表达式
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     */
    private Value buildMulExp(MulExpNode mulExpNode) {
        // 构建操作数和运算符列表
        mulExpNode.populateLists();
        List<UnaryExpNode> unaryExpNodes = mulExpNode.getUnaryExpNodes();
        List<Token> operators = mulExpNode.getOperators();

        // 初始操作数
        Value operand1 = buildUnaryExp(unaryExpNodes.get(0));

        // 遍历后续的 UnaryExp 和运算符
        for (int i = 0; i < operators.size(); i++) {
            Value operand2 = buildUnaryExp(unaryExpNodes.get(i + 1));
            Token operator = operators.get(i);

            // 根据操作符生成 ALU 指令
            Alu.OP op = switch (operator.getType()) {
                case MULT -> Alu.OP.MUL;
                case DIV -> Alu.OP.SDIV;
                case MOD -> Alu.OP.SREM;
                default -> throw new IllegalStateException("Unexpected value: " + operator.getType());
            };
            Alu aluInstr = new Alu(tempName + getVarId(), operand1, operand2, op, curBlock);
            curBlock.addInstr(aluInstr);

            // 更新操作数
            operand1 = aluInstr;
        }

        return operand1;
    }



    /**
     * 解析一元表达式，包括基本表达式、函数调用和一元运算符操作。
     * @param unaryExpNode 一元表达式的节点。
     * @return 解析后生成的LLVM IR值。
     */
    private Value buildUnaryExp(UnaryExpNode unaryExpNode) {
        // 如果是基本表达式（PrimaryExp）
        if (unaryExpNode.getPrimaryExpNode() != null) {
            // 直接构建基本表达式并返回结果
            return buildPrimaryExp(unaryExpNode.getPrimaryExpNode());
        }
        else if (unaryExpNode.getIdent() != null) {
            // 函数调用
            String functionName = unaryExpNode.getIdent();

            // 获取函数定义
            Function function = ((FunctionSymbol) symbolTable.lookup(functionName)).getLLVMIR();

            // 获取形参类型列表
            List<LLVMType> paramTypes = function.getParams().stream()
                    .map(Param::getType)
                    .toList();

            // 构建实参
            ArrayList<Value> parameters = new ArrayList<>();
            if (unaryExpNode.getFuncRParamsNode() != null) {
                parameters = buildFuncRParams(unaryExpNode.getFuncRParamsNode(), paramTypes);
            }

            // 创建函数调用指令
            Call callInstr = new Call(function, tempName + getVarId(), parameters, curBlock);
            curBlock.addInstr(callInstr);

            // 转换返回值类型（如有必要）
            return convertType(callInstr, LLVMType.Int32);
        }
        // 如果是一元运算符
        else if (unaryExpNode.getUnaryExpNode() != null) {
            // 获取一元运算符类型
            TokenType operatorType = unaryExpNode.getUnaryOpNode().getToken().getType();
            // 构建一元表达式的操作数
            Value operand = buildUnaryExp(unaryExpNode.getUnaryExpNode());

//            // 如果操作数类型为char（Int8），则首先进行类型扩展
//            if (operand.getType() == LLVMType.Int8) {
//                operand = new Zext(tempName + getVarId(), operand, curBlock, LLVMType.Int32);
//                curBlock.addInstr((Instruction) operand);
//            }

            // 根据一元运算符类型进行相应的IR指令构建
            if (operatorType == TokenType.PLUS) {
                // 一元加运算符，通常无需操作，直接返回操作数
                return operand;
            } else if (operatorType == TokenType.MINU) {
                // 一元减运算符，生成取负指令
                Alu negativeInstr = new Alu(tempName + getVarId(), new Constant(0), operand, Alu.OP.SUB, curBlock);
                curBlock.addInstr(negativeInstr);
                return negativeInstr;
            } else if (operatorType == TokenType.NOT) {
                // 一元非运算符，先比较是否为0，然后扩展结果
                Icmp cmpInstr = new Icmp(operand, new Constant(0), tempName + getVarId(), curBlock, Icmp.OP.EQ);
                curBlock.addInstr(cmpInstr);
                Zext zextInstr = new Zext(tempName + getVarId(), cmpInstr, curBlock, LLVMType.Int32);
                curBlock.addInstr(zextInstr);
                return zextInstr;
            }
        }

        // 如果没有匹配的类型，返回null
        return null;
    }



    /**
     * 遍历基本表达式
     * PrimaryExp → '(' Exp ')' | LVal | Number | Character
     */
    /**
     * 解析基本表达式，包括括号内表达式、变量引用、整数常量和字符常量。
     * @param primaryExpNode 基本表达式节点。
     * @return 生成的LLVM IR值。
     */
    private Value buildPrimaryExp(PrimaryExpNode primaryExpNode) {
        // 处理括号内的表达式
        if (primaryExpNode.getExpNode() != null) {
            return buildExp(primaryExpNode.getExpNode());
        }
        // 处理变量或数组引用
        else if (primaryExpNode.getlValNode() != null) {
            return buildLValForValue(primaryExpNode.getlValNode());
        }
        // 处理整数常量
        else if (primaryExpNode.getNumberNode() != null) {
            return new Constant(primaryExpNode.getNumberNode().getIntConst());
        }
        // 处理字符常量
        else if (primaryExpNode.getCharacterNode() != null) {
            // 提取字符常量字符串中的第一个字符，并转换为整数值,注意处理转义字符
            // 提取字符常量字符串中的第一个字符，并转换为整数值,注意处理转义字符
            String charConst = primaryExpNode.getCharacterNode().getCharConst();
            int charValue;
            if (charConst.charAt(1) == '\\') {
                switch (charConst.charAt(2)) {
                    case 'a': charValue = '\u0007'; break; // 警报字符
                    case 'b': charValue = '\b'; break;    // 退格
                    case 't': charValue = '\t'; break;    // 水平制表符
                    case 'n': charValue = '\n'; break;    // 换行
                    case 'v': charValue = '\u000B'; break; // 垂直制表符
                    case 'f': charValue = '\f'; break;    // 换页
                    case 'r': charValue = '\r'; break;    // 回车
                    case '\\': charValue = '\\'; break;   // 反斜杠
                    case '\'': charValue = '\''; break;   // 单引号
                    case '"': charValue = '\"'; break;    // 双引号
                    case '0': charValue = '\0'; break;    // 空字符
                    default: throw new IllegalArgumentException("Unsupported escape sequence: \\" + charConst.charAt(2));
                }
            } else {
                charValue = charConst.charAt(1);
            }
            // 创建并返回字符常量，通常需要将字符常量值截取为8位整数
            return new Constant(charValue & 0xFF);
        }

        return null;  // 如果没有匹配的类型，返回null
    }




    /**
     * 构建用于从变量或数组加载值的LLVM IR代码。
     * @param lValNode LVal节点，表示变量或数组访问。
     * @return LLVM IR值，表示加载的变量或数组元素。
     */
    private Value buildLValForValue(LValNode lValNode) {
        // 从符号表中查找变量或数组的符号信息
        VariableSymbol symbol = lValNode.getVariableSymbol();
        Value baseValue = symbol.getLLVMIR();

        if (symbol.getDimension() == 0) {
            // 标量：直接加载值
            Load loadInstr = new Load(tempName + getVarId(), baseValue, curBlock);
            curBlock.addInstr(loadInstr);

            // 使用统一的类型转换方法
            return convertType(loadInstr, LLVMType.Int32);
        } else if (symbol.getDimension() == 1) {
            // 一维数组
            if (lValNode.getExpNode() != null) {
                // 数组元素访问：通过索引访问特定元素
                Value index = buildExp(lValNode.getExpNode());

                // 计算数组元素的指针
                GetPtr getPtrInstr = new GetPtr(tempName + getVarId(), baseValue, index, curBlock);
                curBlock.addInstr(getPtrInstr);

                // 加载数组元素的值
                Load loadInstr = new Load(tempName + getVarId(), getPtrInstr, curBlock);
                curBlock.addInstr(loadInstr);

                // 返回加载的值
                return convertType(loadInstr, LLVMType.Int32);
            } else {
                // 数组传参：返回数组的首地址
                return baseValue;
            }
        } else {
            throw new UnsupportedOperationException("Only scalar and one-dimensional arrays are supported.");
        }
    }
    // ForStmt → LVal '=' Exp
    public void buildForStmt(ForStmtNode forStmtNode, Token funcTypeToken) {
        Value value1 = buildLValForAssign(forStmtNode.getlValNode());
        Value value2 = buildExp(forStmtNode.getExpNode());
        LLVMType targetType = ((PointerType) value1.getType()).getPointedType();
        Value convertedValue = convertType(value2, targetType);
        curBlock.addInstr(new Store(convertedValue,value1, curBlock));
    }
    //条件表达式 Cond → LOrExp // 存在即可
    public void buildCond(CondNode condNode,BasicBlock thenBlock, BasicBlock elseBlock){
        buildLOrExp(condNode.getlOrExpNode(),thenBlock,elseBlock);
    }
    //RelExp → AddExp | RelExp ('<' | '<=' | '>' | '>=') AddExp
    public Value buildRelExp(RelExpNode relExpNode) {
        // 构建操作数和运算符列表
        relExpNode.populateLists();
        List<AddExpNode> addExpNodes = relExpNode.getAddExpNodes();
        List<Token> operators = relExpNode.getOperators();

        // 初始操作数
        Value operand1 = buildAddExp(addExpNodes.get(0));
        if(operators.size() == 0){
            return operand1;
        }
        // 遍历后续的 AddExp 和运算符
        for (int i = 0; i < operators.size(); i++) {
            Value operand2 = buildAddExp(addExpNodes.get(i + 1));
            Token operator = operators.get(i);
            if(operand1.getType() == LLVMType.Int1){
                operand1 = new Zext(tempName + getVarId(), operand1, curBlock, LLVMType.Int32);
                curBlock.addInstr((Instruction) operand1);
            }
            if(operand2.getType() == LLVMType.Int1){
                operand2 = new Zext(tempName + getVarId(), operand2, curBlock, LLVMType.Int32);
                curBlock.addInstr((Instruction) operand2);
            }
            // 根据操作符生成比较指令
            Icmp.OP op = switch (operator.getType()) {
                case LSS -> Icmp.OP.SLT;
                case LEQ -> Icmp.OP.SLE;
                case GRE -> Icmp.OP.SGT;
                case GEQ -> Icmp.OP.SGE;
                default -> throw new IllegalStateException("Unexpected value: " + operator.getType());
            };
            Icmp icmpInstr = new Icmp(operand1, operand2, tempName + getVarId(), curBlock, op);
            curBlock.addInstr(icmpInstr);

            // 更新操作数
            operand1 = icmpInstr;
        }

        return operand1;
    }


    //  LAndExp → EqExp | LAndExp '&&' EqExp
    public void buildLAndExp(LAndExpNode lAndExpNode, BasicBlock thenBlock, BasicBlock elseBlock) {
        lAndExpNode.populateLists();
        List<EqExpNode> eqExpNodes = lAndExpNode.getEqExpNodes();
        List<String> operators = lAndExpNode.getOperators();
        if (operators.isEmpty()) {
            buildEqExp(eqExpNodes.get(0), thenBlock, elseBlock);
            return;
        }
        for (int i = 0; i < operators.size(); i++) {
            BasicBlock nextBlock = new BasicBlock(blockName + getBlockId(), curFunc);
            curFunc.addBasicBlock(nextBlock);
            buildEqExp(eqExpNodes.get(i), nextBlock, elseBlock);
            curBlock = nextBlock;
        }
        buildEqExp(eqExpNodes.get(eqExpNodes.size() - 1), thenBlock, elseBlock);
    }
    //LOrExp → LAndExp | LOrExp '||' LAndExp
    public void buildLOrExp(LOrExpNode lOrExpNode, BasicBlock thenBlock, BasicBlock elseBlock) {
        lOrExpNode.populateLists();
        List<LAndExpNode> lAndExpNodes = lOrExpNode.getlAndExpNodes();
        List<String> operators = lOrExpNode.getOperators();
        if (operators.isEmpty()) {
            buildLAndExp(lAndExpNodes.get(0), thenBlock, elseBlock);
            return;
        }
        for (int i = 0; i < operators.size(); i++) {
            BasicBlock nextBlock = new BasicBlock(blockName + getBlockId(), curFunc);
            curFunc.addBasicBlock(nextBlock);
            buildLAndExp(lAndExpNodes.get(i), thenBlock, nextBlock);
            curBlock = nextBlock;
        }
        buildLAndExp(lAndExpNodes.get(lAndExpNodes.size() - 1), thenBlock, elseBlock);
    }
    //EqExp → RelExp | EqExp ('==' | '!=') RelExp
    public void buildEqExp(EqExpNode eqExpNode, BasicBlock thenBlock, BasicBlock elseBlock) {
        // 构建操作数和运算符列表
        eqExpNode.populateLists();
        List<RelExpNode> relExpNodes = eqExpNode.getRelExpNodes();
        List<Token> operators = eqExpNode.getOperators();

        // 初始操作数
        Value operand1 = buildRelExp(relExpNodes.get(0));
        if (operators.isEmpty()) {
            Value result;
            if(operand1.getType() != LLVMType.Int1){
                result = new Icmp(new Constant(0),operand1,tempName + getVarId(),curBlock,Icmp.OP.NE);
                curBlock.addInstr((Instruction) result);
            }else {
                result = operand1;
            }
            curBlock.addInstr(new Branch(result, thenBlock, elseBlock, curBlock));
            return;
        }
        // 遍历后续的 RelExp 和运算符
        for (int i = 0; i < operators.size(); i++) {
            Value operand2 = buildRelExp(relExpNodes.get(i + 1));
            Token operator = operators.get(i);

            // 根据操作符生成比较指令
            Icmp.OP op = switch (operator.getType()) {
                case EQL -> Icmp.OP.EQ;
                case NEQ -> Icmp.OP.NE;
                default -> throw new IllegalStateException("Unexpected value: " + operator.getType());
            };
            //如果是bool类型，需要转换为int类型
            if(operand1.getType() == LLVMType.Int1){
                operand1 = new Zext(tempName + getVarId(), operand1, curBlock, LLVMType.Int32);
                curBlock.addInstr((Instruction) operand1);
            }
            if(operand2.getType() == LLVMType.Int1){
                operand2 = new Zext(tempName + getVarId(), operand2, curBlock, LLVMType.Int32);
                curBlock.addInstr((Instruction) operand2);
            }
            Icmp icmpInstr = new Icmp(operand1, operand2, tempName + getVarId(), curBlock, op);
            curBlock.addInstr(icmpInstr);

            // 更新操作数
            operand1 = icmpInstr;
        }

        // 根据结果跳转到不同的基本块
        curBlock.addInstr(new Branch( operand1,thenBlock,elseBlock, curBlock));
    }


    private Value convertType(Value value, LLVMType targetType) {
        if (value.getType().equals(targetType) || value.getType() == LLVMType.Void) {
            return value; // 类型相同，无需转换
        }else if (value.getType() instanceof PointerType && targetType instanceof PointerType) {
            // 如果两者都是指针类型，且指向的类型一致，无需转换
            return value;
        }else if (value.getType().isBiggerThan(targetType)) {
            // 需要截断，添加 trunc 指令
            Trunc truncInstr = new Trunc(tempName + getVarId(), value, curBlock, targetType);
            curBlock.addInstr(truncInstr);
            return truncInstr;
        } else {
            // 需要扩展，添加 zext 指令
            Zext zextInstr = new Zext(tempName + getVarId(), value, curBlock, targetType);
            curBlock.addInstr(zextInstr);
            return zextInstr;
        }
    }


}