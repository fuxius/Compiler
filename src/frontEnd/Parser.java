package frontEnd;

import token.Token;
import token.TokenManager;
import token.TokenType;
import error.ErrorHandler;
import error.ErrorType;
import ast.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class Parser {
    private static final Parser instance = new Parser(TokenManager.getInstance());
    private List<String> parserOutputs = new ArrayList<>();

    public static Parser getInstance() {
        return instance;
    }

    private TokenManager tokenManager; // Token管理器，用于获取和管理Token
    private Token currentToken; // 当前的Token
    private Token prevToken; // 上一个Token

    private BufferedWriter parserWriter; // 用于输出到 parser.txt

    public Parser(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.currentToken = tokenManager.getToken(); // 初始化当前Token
        this.prevToken = null; // 初始化prevToken
        try {
            parserWriter = new BufferedWriter(new FileWriter("parser.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 更新当前Token和prevToken
    private void updateCurrentToken() {
        prevToken = currentToken; // 更新prevToken为当前Token
        currentToken = tokenManager.getToken(); // 获取下一个Token
    }

    // 匹配并匹配预期的Token
    private Token match(TokenType expectedType) {
        if (currentToken != null && currentToken.getType() == expectedType) {
            Token matchedToken = currentToken;
            outputToken(matchedToken); // 输出Token
            tokenManager.nextToken();
            updateCurrentToken();
            return matchedToken;
        } else {
            // 为了继续解析，跳过当前Token
            tokenManager.nextToken();
            updateCurrentToken();
            return null;
        }
    }

    // 判断当前Token是否是指定类型
    private boolean checkToken(TokenType type) {
        return currentToken != null && currentToken.getType() == type;
    }

    // 输出Token
    private void outputToken(Token token) {
        try {
            parserWriter.write(token.getType() + " " + token.getValue() + "\n");
            parserOutputs.add(token.getType() + " " + token.getValue() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 输出语法成分
    private void outputGrammar(String grammar) {
        try {
            parserWriter.write(grammar + "\n");
            parserOutputs.add(grammar + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 报告错误，传入发生错误的Token
    private void reportError(Token token, ErrorType errorType) {

        ErrorHandler.getInstance().reportError(token.getLine(), errorType);

    }

    // 主入口：解析编译单元
    public CompUnitNode parseCompUnit() {
        List<DeclNode> declNodes = new ArrayList<>();
        List<FuncDefNode> funcDefNodes = new ArrayList<>();
        MainFuncDefNode mainFuncDefNode = null;

        // 解析{Decl}
        while (tokenManager.lookAhead(1).getType() != TokenType.MAINTK && tokenManager.lookAhead(2).getType() != TokenType.LPARENT) {
            DeclNode declNode = parseDecl();
            if (declNode != null) {
                declNodes.add(declNode);
            }
        }

        // 解析{FuncDef}
        while (tokenManager.lookAhead( 1).getType() != TokenType.MAINTK) {
            FuncDefNode funcDefNode = parseFuncDef();
            if (funcDefNode != null) {
                funcDefNodes.add(funcDefNode);
            }
        }

        // 解析MainFuncDef
        mainFuncDefNode = parseMainFuncDef();


        CompUnitNode compUnitNode = new CompUnitNode(declNodes, funcDefNodes, mainFuncDefNode);
        outputGrammar("<CompUnit>");
        closeParserWriter();
        return compUnitNode;
    }

    // 判断是否是Decl的开始
    private boolean isDecl() {
        // 如果是常量声明，返回 true
        if (checkToken(TokenType.CONSTTK)) {
            return true;
        }
        // 检查是否是变量声明
        if (checkToken(TokenType.INTTK) || checkToken(TokenType.CHARTK)) {
            // 检查下一个 token 是否是标识符（变量名）
            return true;
        }
        // 如果不符合声明的特征，返回 false
        return false;
    }


    // 解析Decl
    private DeclNode parseDecl() {
        if (checkToken(TokenType.CONSTTK)) {
            // 常量声明
            ConstDeclNode constDeclNode = parseConstDecl();
            return new DeclNode(constDeclNode);
        } else if (checkToken(TokenType.INTTK) || checkToken(TokenType.CHARTK)) {
            // 变量声明
            VarDeclNode varDeclNode = parseVarDecl();
            return new DeclNode(varDeclNode);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }

    // 解析ConstDecl
    //常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' // i
    private ConstDeclNode parseConstDecl() {
        match(TokenType.CONSTTK); // 匹配'const'
        BTypeNode bTypeNode = parseBType();
        List<ConstDefNode> constDefNodes = new ArrayList<>();
        ConstDefNode constDefNode = parseConstDef();
        if (constDefNode != null) {
            constDefNodes.add(constDefNode);
        }
        while (checkToken(TokenType.COMMA)) {
            match(TokenType.COMMA); // 匹配','
            constDefNode = parseConstDef();
            if (constDefNode != null) {
                constDefNodes.add(constDefNode);
            }
        }
        if (!checkToken(TokenType.SEMICN)) {
            reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少分号，使用prevToken
        } else {
            match(TokenType.SEMICN); // 匹配';'
        }
        outputGrammar("<ConstDecl>");
        return new ConstDeclNode(bTypeNode, constDefNodes);
    }

    // 解析BType
    private BTypeNode parseBType() {
        if (checkToken(TokenType.INTTK)) {
            Token IntToken = match(TokenType.INTTK);
            return new BTypeNode(IntToken);
        } else if (checkToken(TokenType.CHARTK)) {
            Token CharToken = match(TokenType.CHARTK);
            return new BTypeNode(CharToken);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }


    // 解析ConstDef
    private ConstDefNode parseConstDef() {
        // 常量定义 ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        if (checkToken(TokenType.IDENFR)) {
            Token identToken = match(TokenType.IDENFR);
            ConstExpNode constExpNode = null;
            // 解析零个或一个 '[' ConstExp ']'
            if (checkToken(TokenType.LBRACK)) {
                match(TokenType.LBRACK); // 匹配'['
                constExpNode = parseConstExp();
                if (!checkToken(TokenType.RBRACK)) {
                    reportError(prevToken, ErrorType.MISSING_RIGHT_RBRACK); // 缺少']'，使用prevToken
                } else {
                    match(TokenType.RBRACK); // 匹配']'
                }
            }
            match(TokenType.ASSIGN); // 匹配'='
            ConstInitValNode constInitValNode = parseConstInitVal();
            outputGrammar("<ConstDef>");
            return new ConstDefNode(identToken, constExpNode, constInitValNode);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }


    // 解析ConstInitVal
    private ConstInitValNode parseConstInitVal() {
        if (checkToken(TokenType.LBRACE)) {
            // '{' [ConstExp { ',' ConstExp }] '}'
            match(TokenType.LBRACE); // 匹配'{'
            List<ConstExpNode> constExpNodeList = new ArrayList<>();
            if (!checkToken(TokenType.RBRACE)) {
                ConstExpNode constExpNode = parseConstExp();
                if (constExpNode != null) {
                    constExpNodeList.add(constExpNode);
                }
                while (checkToken(TokenType.COMMA)) {
                    match(TokenType.COMMA); // 匹配','
                    constExpNode = parseConstExp();
                    if (constExpNode != null) {
                        constExpNodeList.add(constExpNode);
                    }
                }
            }
            match(TokenType.RBRACE); // 匹配'}'

            outputGrammar("<ConstInitVal>");
            return new ConstInitValNode(constExpNodeList);
        } else if (checkToken(TokenType.STRCON)) {
            // StringConst
            Token strToken = match(TokenType.STRCON);
            outputGrammar("<ConstInitVal>");
            return new ConstInitValNode(strToken);
        } else {
            // ConstExp
            ConstExpNode constExpNode = parseConstExp();
            outputGrammar("<ConstInitVal>");
            return new ConstInitValNode(constExpNode);
        }
    }

    // 解析ConstExp
    private ConstExpNode parseConstExp() {
        AddExpNode addExpNode = parseAddExp();
        outputGrammar("<ConstExp>");
        return new ConstExpNode(addExpNode);
    }

    // 解析AddExp
    private AddExpNode parseAddExp() {
        MulExpNode mulExpNode = parseMulExp();
        AddExpNode addExpNode = new AddExpNode(mulExpNode);
        while (checkToken(TokenType.PLUS) || checkToken(TokenType.MINU)) {
            outputGrammar("<AddExp>");
            Token opToken = currentToken;
            match(currentToken.getType()); // 匹配'+' 或 '-'
            MulExpNode nextMulExpNode = parseMulExp();
            addExpNode = new AddExpNode(addExpNode, opToken, nextMulExpNode);
        }
        outputGrammar("<AddExp>");
        return addExpNode;
    }

    // 解析MulExp
    private MulExpNode parseMulExp() {
        UnaryExpNode unaryExpNode = parseUnaryExp();
        MulExpNode mulExpNode = new MulExpNode(unaryExpNode);
        while (checkToken(TokenType.MULT) || checkToken(TokenType.DIV) || checkToken(TokenType.MOD)) {
            outputGrammar("<MulExp>");
            Token opToken = currentToken;
            match(currentToken.getType()); // 匹配'*'、'/'、'%'
            UnaryExpNode nextUnaryExpNode = parseUnaryExp();
            mulExpNode = new MulExpNode(mulExpNode, opToken, nextUnaryExpNode);
        }
        outputGrammar("<MulExp>");
        return mulExpNode;
    }

    // 解析UnaryExp
    private UnaryExpNode parseUnaryExp() {
        if (checkToken(TokenType.IDENFR)) {
            // 可能是函数调用或变量
            Token identToken = currentToken;
            if (tokenManager.lookAhead(1) != null && tokenManager.lookAhead(1).getType() == TokenType.LPARENT) {
                match(TokenType.IDENFR);
                // 函数调用
                match(TokenType.LPARENT); // 匹配'('
                FuncRParamsNode funcRParamsNode = null;
                if (isExpStartingToken(currentToken)) {
                    funcRParamsNode = parseFuncRParams();
                }
                if (!checkToken(TokenType.RPARENT)) {
                    reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
                } else {
                    match(TokenType.RPARENT); // 匹配')'
                }
                UnaryExpNode unaryExpNode = new UnaryExpNode(identToken, funcRParamsNode);
                outputGrammar("<UnaryExp>");
                return unaryExpNode;
            } else {
                // 变量，当作 PrimaryExp 处理
                PrimaryExpNode primaryExpNode = parsePrimaryExp();
                UnaryExpNode unaryExpNode = new UnaryExpNode(primaryExpNode);
                outputGrammar("<UnaryExp>");
                return unaryExpNode;
            }
        } else if (checkToken(TokenType.PLUS) || checkToken(TokenType.MINU) || checkToken(TokenType.NOT)) {
            // 一元运算符
            UnaryOpNode unaryOpNode = parseUnaryOp();
            UnaryExpNode nextUnaryExpNode = parseUnaryExp();
            UnaryExpNode unaryExpNode = new UnaryExpNode(unaryOpNode, nextUnaryExpNode);
            outputGrammar("<UnaryExp>");
            return unaryExpNode;
        } else {
            // PrimaryExp
            PrimaryExpNode primaryExpNode = parsePrimaryExp();
            if (primaryExpNode != null) {
                UnaryExpNode unaryExpNode = new UnaryExpNode(primaryExpNode);
                outputGrammar("<UnaryExp>");
                return unaryExpNode;
            } else {
                // 无法解析的表达式，报告错误
//                reportError(currentToken, ErrorType.ILLEGAL_SYMBOL);
                return null;
            }
        }
    }
    private boolean isExpStartingToken(Token token) {
        if (token == null) {
            return false;
        }
        switch (token.getType()) {
            case IDENFR: // 标识符
            case INTCON: // 整数常量
            case CHRCON: // 字符常量
            case STRCON: // 字符串常量
            case LPARENT: // 左括号
            case PLUS:    // 加号
            case MINU:    // 减号
            case NOT:     // 逻辑非
                return true;
            default:
                return false;
        }
    }

    // 解析UnaryOp
    private UnaryOpNode parseUnaryOp() {
       Token opToken = currentToken;
       match(currentToken.getType()); // 匹配一元运算符
       // 输出 <UnaryOp>
       outputGrammar("<UnaryOp>");
       return new UnaryOpNode(opToken);

    }
    // 解析PrimaryExp
    private PrimaryExpNode parsePrimaryExp() {
        if (checkToken(TokenType.LPARENT)) {
            // '(' Exp ')'
            match(TokenType.LPARENT); // 匹配'('
            ExpNode expNode = parseExp();
            if (!checkToken(TokenType.RPARENT)) {
                reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
            } else {
                match(TokenType.RPARENT); // 匹配')'
            }
            PrimaryExpNode primaryExpNode = new PrimaryExpNode(expNode);
            outputGrammar("<PrimaryExp>");
            return primaryExpNode;
        } else if (checkToken(TokenType.IDENFR)) {
            // LVal
            LValNode lValNode = parseLVal();
            PrimaryExpNode primaryExpNode = new PrimaryExpNode(lValNode);
            outputGrammar("<PrimaryExp>");
            return primaryExpNode;
        } else if (checkToken(TokenType.INTCON)) {
            // Number
            NumberNode numberNode = parseNumber();
            PrimaryExpNode primaryExpNode = new PrimaryExpNode(numberNode);
            outputGrammar("<PrimaryExp>");
            return primaryExpNode;
        } else if (checkToken(TokenType.CHRCON)) {
            // Character
            CharacterNode characterNode = parseCharacter();
            PrimaryExpNode primaryExpNode = new PrimaryExpNode(characterNode);
            outputGrammar("<PrimaryExp>");
            return primaryExpNode;
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }
    // 解析Number
    private NumberNode parseNumber() {
        Token intToken = match(TokenType.INTCON);
        NumberNode numberNode = new NumberNode(intToken);
        outputGrammar("<Number>");
        return numberNode;
    }
    // 解析Character
    private CharacterNode parseCharacter() {
         Token charToken = match(TokenType.CHRCON);
         CharacterNode characterNode = new CharacterNode(charToken);
         outputGrammar("<Character>");
         return characterNode;
    }


    // 解析LVal
    private LValNode parseLVal() {

        Token identToken = match(TokenType.IDENFR);
        if (identToken == null) {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }

        ExpNode expNode = null;
        if (checkToken(TokenType.LBRACK)) {
            match(TokenType.LBRACK); // 匹配'['
            expNode = parseExp();
            if (!checkToken(TokenType.RBRACK)) {
                reportError(prevToken, ErrorType.MISSING_RIGHT_RBRACK); // 缺少']'，使用prevToken
            } else {
                match(TokenType.RBRACK); // 匹配']'
            }
        }
        LValNode lValNode = new LValNode(identToken, expNode);
        outputGrammar("<LVal>");
        return lValNode;
    }

    // 解析FuncRParams
    private FuncRParamsNode parseFuncRParams() {
        List<ExpNode> expNodes = new ArrayList<>();
        ExpNode expNode = parseExp();
        if (expNode != null) {
            expNodes.add(expNode);
        }
        while (checkToken(TokenType.COMMA)) {
            match(TokenType.COMMA); // 匹配','
            expNode = parseExp();
            if (expNode != null) {
                expNodes.add(expNode);
            }
        }
        outputGrammar("<FuncRParams>");
        return new FuncRParamsNode(expNodes);
    }

    // 解析Exp
    private ExpNode parseExp() {
        AddExpNode addExpNode = parseAddExp();
        ExpNode expNode = new ExpNode(addExpNode);
        outputGrammar("<Exp>");
        return expNode;
    }

    // 解析VarDecl
    private VarDeclNode parseVarDecl() {
        BTypeNode bTypeNode = parseBType();
        List<VarDefNode> varDefNodes = new ArrayList<>();
        VarDefNode varDefNode = parseVarDef();
        if (varDefNode != null) {
            varDefNodes.add(varDefNode);
        }
        while (checkToken(TokenType.COMMA)) {
            match(TokenType.COMMA); // 匹配','
            varDefNode = parseVarDef();
            if (varDefNode != null) {
                varDefNodes.add(varDefNode);
            }
        }
        if (!checkToken(TokenType.SEMICN)) {
            reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少分号，使用prevToken
        } else {
            match(TokenType.SEMICN); // 匹配';'
        }
        outputGrammar("<VarDecl>");
        return new VarDeclNode(bTypeNode, varDefNodes);
    }

    // 解析VarDef
    private VarDefNode parseVarDef() {
        if (checkToken(TokenType.IDENFR)) {
            Token identToken = match(TokenType.IDENFR);
            ConstExpNode constExpNode = null;
            if (checkToken(TokenType.LBRACK)) {
                match(TokenType.LBRACK); // 匹配'['
                constExpNode = parseConstExp();
                if (!checkToken(TokenType.RBRACK)) {
                    reportError(prevToken, ErrorType.MISSING_RIGHT_RBRACK); // 缺少']'，使用prevToken
                } else {
                    match(TokenType.RBRACK); // 匹配']'
                }
            }
            InitValNode initValNode = null;
            if (checkToken(TokenType.ASSIGN)) {
                match(TokenType.ASSIGN); // 匹配'='
                initValNode = parseInitVal();
            }
            VarDefNode varDefNode = new VarDefNode(identToken, constExpNode, initValNode);
            outputGrammar("<VarDef>");
            return varDefNode;
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }

    // 解析InitVal
    private InitValNode parseInitVal() {
        if (checkToken(TokenType.LBRACE)) {
            // '{' [Exp { ',' Exp }] '}'
            match(TokenType.LBRACE); // 匹配'{'
            List<ExpNode> expNodeList = new ArrayList<>();
            if (!checkToken(TokenType.RBRACE)) {
                ExpNode expNode = parseExp();
                if (expNode != null) {
                    expNodeList.add(expNode);
                }
                while (checkToken(TokenType.COMMA)) {
                    match(TokenType.COMMA); // 匹配','
                    expNode = parseExp();
                    if (expNode != null) {
                        expNodeList.add(expNode);
                    }
                }
            }

            match(TokenType.RBRACE); // 匹配'}'

            outputGrammar("<InitVal>");
            return new InitValNode(expNodeList);
        } else if (checkToken(TokenType.STRCON)) {
            // StringConst
            Token strToken = match(TokenType.STRCON);
            outputGrammar("<InitVal>");
            return new InitValNode(strToken);
        } else {
            // Exp
            ExpNode expNode = parseExp();
            outputGrammar("<InitVal>");
            return new InitValNode(expNode);
        }
    }

    // 解析FuncDef
    private FuncDefNode parseFuncDef() {
        FuncTypeNode funcTypeNode = parseFuncType();
        if (checkToken(TokenType.IDENFR)) {
            Token identToken = match(TokenType.IDENFR);
            match(TokenType.LPARENT); // 匹配'('
            FuncFParamsNode funcFParamsNode = null;
            if (!checkToken(TokenType.RPARENT)) {
                funcFParamsNode = parseFuncFParams();
            }
            if (!checkToken(TokenType.RPARENT)) {
                reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
            } else {
                match(TokenType.RPARENT); // 匹配')'
            }
            BlockNode blockNode = parseBlock();
            outputGrammar("<FuncDef>");
            return new FuncDefNode(funcTypeNode, identToken, funcFParamsNode, blockNode);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }

    // 解析FuncType
    private FuncTypeNode parseFuncType() {
        if (checkToken(TokenType.VOIDTK)) {
            Token voidToken = match(TokenType.VOIDTK);
            outputGrammar("<FuncType>");
            return new FuncTypeNode(voidToken);
        } else if (checkToken(TokenType.INTTK)) {
            Token intToken = match(TokenType.INTTK);
            outputGrammar("<FuncType>");
            return new FuncTypeNode(intToken);
        } else if (checkToken(TokenType.CHARTK)) {
            Token charToken = match(TokenType.CHARTK);
            outputGrammar("<FuncType>");
            return new FuncTypeNode(charToken);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }

    // 解析FuncFParams
    private FuncFParamsNode parseFuncFParams() {
        List<FuncFParamNode> funcFParamNodes = new ArrayList<>();
        FuncFParamNode funcFParamNode = parseFuncFParam();
        if (funcFParamNode != null) {
            funcFParamNodes.add(funcFParamNode);
        }
        while (checkToken(TokenType.COMMA)) {
            match(TokenType.COMMA); // 匹配','
            funcFParamNode = parseFuncFParam();
            if (funcFParamNode != null) {
                funcFParamNodes.add(funcFParamNode);
            }
        }
        outputGrammar("<FuncFParams>");
        return new FuncFParamsNode(funcFParamNodes);
    }

    // 解析FuncFParam
    private FuncFParamNode parseFuncFParam() {
        BTypeNode bTypeNode = parseBType();
        if (checkToken(TokenType.IDENFR)) {
            Token identToken = match(TokenType.IDENFR);
            boolean isArray = false;
            if (checkToken(TokenType.LBRACK)) {
                match(TokenType.LBRACK); // 匹配'['
                if (!checkToken(TokenType.RBRACK)) {
                    reportError(prevToken, ErrorType.MISSING_RIGHT_RBRACK); // 缺少']'，使用prevToken
                } else {
                    match(TokenType.RBRACK); // 匹配']'
                }
                isArray = true;
            }
            outputGrammar("<FuncFParam>");
            return new FuncFParamNode(bTypeNode, identToken, isArray);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }

    // 解析Block
    private BlockNode parseBlock() {
        Token RbraceToken = null;
        match(TokenType.LBRACE); // 匹配'{'
        List<BlockItemNode> blockItemNodes = new ArrayList<>();
        while (!checkToken(TokenType.RBRACE) && currentToken != null) {
            BlockItemNode blockItemNode = parseBlockItem();
            if (blockItemNode != null) {
                blockItemNodes.add(blockItemNode);
            }
        }

        RbraceToken = match(TokenType.RBRACE); // 匹配'}'

        outputGrammar("<Block>");
        return new BlockNode(blockItemNodes,RbraceToken);
    }

    // 解析BlockItem
    private BlockItemNode parseBlockItem() {
        if (isDecl()) {
            DeclNode declNode = parseDecl();
            return new BlockItemNode(declNode);
        } else {
            StmtNode stmtNode = parseStmt();
            return new BlockItemNode(stmtNode);
        }
    }

    // 解析Stmt
    private StmtNode parseStmt() {
        if (checkToken(TokenType.LBRACE)) {
            // Block
            BlockNode blockNode = parseBlock();
            outputGrammar("<Stmt>");
            return new StmtNode(blockNode);
        } else if (checkToken(TokenType.IFTK)) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            match(TokenType.IFTK); // 匹配'if'
            match(TokenType.LPARENT); // 匹配'('
            CondNode condNode = parseCond();
            if (!checkToken(TokenType.RPARENT)) {
                reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
            } else {
                match(TokenType.RPARENT); // 匹配')'
            }
            StmtNode thenStmt = parseStmt();
            StmtNode elseStmt = null;
            if (checkToken(TokenType.ELSETK)) {
                match(TokenType.ELSETK); // 匹配'else'
                elseStmt = parseStmt();
            }
            outputGrammar("<Stmt>");
            return new StmtNode(condNode, thenStmt, elseStmt);
        } else if (checkToken(TokenType.FORTK)) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            match(TokenType.FORTK); // 匹配'for'
            match(TokenType.LPARENT); // 匹配'('
            ForStmtNode forInit = null;
            if (!checkToken(TokenType.SEMICN)) {
                forInit = parseForStmt();
            }
            if (!checkToken(TokenType.SEMICN)) {
                reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少';'，使用prevToken
            } else {
                match(TokenType.SEMICN); // 匹配';'
            }
            CondNode condNode = null;
            if (!checkToken(TokenType.SEMICN)) {
                condNode = parseCond();
            }
            if (!checkToken(TokenType.SEMICN)) {
                reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少';'，使用prevToken
            } else {
                match(TokenType.SEMICN); // 匹配';'
            }
            ForStmtNode forUpdate = null;
            if (!checkToken(TokenType.RPARENT)) {
                forUpdate = parseForStmt();
            }
            if (!checkToken(TokenType.RPARENT)) {
                reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
            } else {
                match(TokenType.RPARENT); // 匹配')'
            }
            StmtNode bodyStmt = parseStmt();
            outputGrammar("<Stmt>");
            return new StmtNode(forInit, condNode, forUpdate, bodyStmt);
        } else if (checkToken(TokenType.BREAKTK)) {
            // 'break' ';'
            Token breakToken = match(TokenType.BREAKTK); // 匹配'break'
            if (!checkToken(TokenType.SEMICN)) {
                reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少';'，使用prevToken
            } else {
                match(TokenType.SEMICN); // 匹配';'
            }
            outputGrammar("<Stmt>");
            return new StmtNode(breakToken);
        } else if (checkToken(TokenType.CONTINUETK)) {
            // 'continue' ';'
            Token continueToken = match(TokenType.CONTINUETK); // 匹配'continue'
            if (!checkToken(TokenType.SEMICN)) {
                reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少';'，使用prevToken
            } else {
                match(TokenType.SEMICN); // 匹配';'
            }
            outputGrammar("<Stmt>");
            return new StmtNode(continueToken);
        } else if (checkToken(TokenType.RETURNTK)) {
            // 'return' [Exp] ';'
            Token returnToken = match(TokenType.RETURNTK); // 匹配'return'
            ExpNode expNode = null;
            if (!checkToken(TokenType.SEMICN)) {
                expNode = parseExp();
            }
            if (!checkToken(TokenType.SEMICN)) {
                reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少';'，使用prevToken
            } else {
                match(TokenType.SEMICN); // 匹配';'
            }
            outputGrammar("<Stmt>");
            return new StmtNode(returnToken, expNode);
        } else if (checkToken(TokenType.PRINTFTK)) {
            // 'printf' '(' StringConst {',' Exp} ')' ';'
            match(TokenType.PRINTFTK); // 匹配'printf'
            match(TokenType.LPARENT); // 匹配'('
            if (!checkToken(TokenType.STRCON)) {
//                reportError(currentToken, ErrorType.UNDEFINED_IDENT); // 缺少格式字符串，使用currentToken
            }
            Token formatStringToken = match(TokenType.STRCON);
            List<ExpNode> expNodes = new ArrayList<>();
            while (checkToken(TokenType.COMMA)) {
                match(TokenType.COMMA); // 匹配','
                ExpNode expNode = parseExp();
                expNodes.add(expNode);
            }
            if (!checkToken(TokenType.RPARENT)) {
                reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
            } else {
                match(TokenType.RPARENT); // 匹配')'
            }
            if (!checkToken(TokenType.SEMICN)) {
                reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少';'，使用prevToken
            } else {
                match(TokenType.SEMICN); // 匹配';'
            }
            outputGrammar("<Stmt>");
            return new StmtNode(formatStringToken, expNodes);
        } else if (checkToken(TokenType.SEMICN)) {
            // 空语句 ';'
            match(TokenType.SEMICN); // 匹配';'
            outputGrammar("<Stmt>");
            return new StmtNode(); // 使用默认构造函数
        } else {
            // 处理赋值语句或者表达式
            int currentIndex = tokenManager.getCurrentPosition();
            boolean isAssignment = false;

            // 获取总令牌数，防止扫描超出范围
            int totalTokens = tokenManager.getTotalTokens(); // 假设有此方法

            // 扫描直到这一行下一个分号，寻找赋值符号 '='
            for (int i = 0; (currentIndex + i) < totalTokens && tokenManager.lookAhead(i).getLine() == currentToken.getLine(); i++) {
                Token token = tokenManager.lookAhead(i);
                if (token.getType() == TokenType.SEMICN) {
                    break;
                }
                if (token.getType() == TokenType.ASSIGN) {
                    isAssignment = true;
                    break;
                }
            }
            if (isAssignment) {
                // 解析赋值语句
                LValNode lValNode = parseLVal(); // 仅在确定是赋值语句时调用
                match(TokenType.ASSIGN); // 匹配'='
                if (checkToken(TokenType.GETINTTK) || checkToken(TokenType.GETCHARTK)) {
                    // LVal '=' 'getint' '(' ')' ';' 或 LVal '=' 'getchar' '(' ')' ';'
                    Token getFuncToken = currentToken;
                    match(currentToken.getType()); // 匹配 'getint' 或 'getchar'
                    match(TokenType.LPARENT); // 匹配 '('
                    if (!checkToken(TokenType.RPARENT)) {
                        reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少 ')'
                    } else {
                        match(TokenType.RPARENT); // 匹配 ')'
                    }
                    if (!checkToken(TokenType.SEMICN)) {
                        reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少 ';'
                    } else {
                        match(TokenType.SEMICN); // 匹配 ';'
                    }
                    outputGrammar("<Stmt>");
                    return new StmtNode(lValNode, getFuncToken.getType().toString());
                } else {
                    // LVal '=' Exp ';'
                    ExpNode expNode = parseExp();
                    if (!checkToken(TokenType.SEMICN)) {
                        reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少 ';'
                    } else {
                        match(TokenType.SEMICN); // 匹配 ';'
                    }
                    outputGrammar("<Stmt>");
                    return new StmtNode(lValNode, expNode);
                }
            } else {
                // 解析 [Exp] ';'
                ExpNode expNode = parseExp();
                if (!checkToken(TokenType.SEMICN)) {
                    reportError(prevToken, ErrorType.MISSING_SEMICOLON); // 缺少 ';'
                } else {
                    match(TokenType.SEMICN); // 匹配 ';'
                }
                outputGrammar("<Stmt>");
                return new StmtNode(expNode);
            }
        }
    }

    // 解析ForStmt
    private ForStmtNode parseForStmt() {
        LValNode lValNode = parseLVal();
        if (lValNode != null && checkToken(TokenType.ASSIGN)) {
            match(TokenType.ASSIGN); // 匹配 '='
            ExpNode expNode = parseExp();
            outputGrammar("<ForStmt>");
            return new ForStmtNode(lValNode, expNode);
        } else {
//            reportError(currentToken, ErrorType.UNDEFINED_IDENT);
            return null;
        }
    }

    // 解析Cond
    private CondNode parseCond() {
        LOrExpNode lOrExpNode = parseLOrExp();
        outputGrammar("<Cond>");
        return new CondNode(lOrExpNode);
    }

    // 解析LOrExp
    private LOrExpNode parseLOrExp() {
        LAndExpNode lAndExpNode = parseLAndExp();
        LOrExpNode lOrExpNode = new LOrExpNode(lAndExpNode);
        while (checkToken(TokenType.OR)) {
            outputGrammar("<LOrExp>");
            match(TokenType.OR); // 匹配'||'
            LAndExpNode nextLAndExpNode = parseLAndExp();
            lOrExpNode = new LOrExpNode(lOrExpNode, nextLAndExpNode);
        }
        outputGrammar("<LOrExp>");
        return lOrExpNode;
    }

    // 解析LAndExp
    private LAndExpNode parseLAndExp() {
        EqExpNode eqExpNode = parseEqExp();
        LAndExpNode lAndExpNode = new LAndExpNode(eqExpNode);
        while (checkToken(TokenType.AND)) {
            outputGrammar("<LAndExp>");
            match(TokenType.AND); // 匹配'&&'
            EqExpNode nextEqExpNode = parseEqExp();
            lAndExpNode = new LAndExpNode(lAndExpNode, nextEqExpNode);
        }
        outputGrammar("<LAndExp>");
        return lAndExpNode;
    }

    // 解析EqExp
    private EqExpNode parseEqExp() {
        RelExpNode relExpNode = parseRelExp();
        EqExpNode eqExpNode = new EqExpNode(relExpNode);
        while (checkToken(TokenType.EQL) || checkToken(TokenType.NEQ)) {
            outputGrammar("<EqExp>");
            Token opToken = currentToken;
            match(currentToken.getType()); // 匹配'==' 或 '!='
            RelExpNode nextRelExpNode = parseRelExp();
            eqExpNode = new EqExpNode(eqExpNode, opToken, nextRelExpNode);
        }
        outputGrammar("<EqExp>");
        return eqExpNode;
    }

    // 解析RelExp
    private RelExpNode parseRelExp() {
        AddExpNode addExpNode = parseAddExp();
        RelExpNode relExpNode = new RelExpNode(addExpNode);
        while (checkToken(TokenType.LSS) || checkToken(TokenType.GRE) || checkToken(TokenType.LEQ) || checkToken(TokenType.GEQ)) {
            outputGrammar("<RelExp>");
            Token opToken = currentToken;
            match(currentToken.getType()); // 匹配关系运算符
            AddExpNode nextAddExpNode = parseAddExp();
            relExpNode = new RelExpNode(relExpNode, opToken, nextAddExpNode);
        }
        outputGrammar("<RelExp>");
        return relExpNode;
    }

    // 解析MainFuncDef
    private MainFuncDefNode parseMainFuncDef() {
        Token Inttoken = match(TokenType.INTTK); // 匹配'int'
        match(TokenType.MAINTK); // 匹配'main'
        match(TokenType.LPARENT); // 匹配'('
        if (!checkToken(TokenType.RPARENT)) {
            reportError(prevToken, ErrorType.MISSING_RIGHT_BRACKET); // 缺少')'，使用prevToken
        } else {
            match(TokenType.RPARENT); // 匹配')'
        }
        BlockNode blockNode = parseBlock();
        outputGrammar("<MainFuncDef>");
        return new MainFuncDefNode(blockNode,Inttoken);
    }

    // 关闭 parser.txt 文件
    private void closeParserWriter() {
        try {
            if (parserWriter != null) {
                parserWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
