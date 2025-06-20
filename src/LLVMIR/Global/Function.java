package LLVMIR.Global;

import LLVMIR.Base.*;
import LLVMIR.Base.Core.User;
import LLVMIR.Base.Core.Value;
import LLVMIR.IRBuilder;
import LLVMIR.Ins.*;
import LLVMIR.Ins.CIo.*;
import LLVMIR.LLVMType.LLVMType;
import backEnd.Base.Register;

import java.util.*;

/**
 * 表示LLVM中的函数
 */
public class Function extends User {
    private final LLVMType returnType;       // 函数的返回类型
    private final List<Param> params;       // 函数的参数列表
    private final ArrayList<BasicBlock> basicBlocks; // 函数包含的基本块
    private int varId;                      // 变量ID计数
    private int blockId;                    // 基本块ID计数
    private HashMap<Value, Register> registerPool = new HashMap<>();    // 使用 HashMap 实现寄存器池
    private HashMap<BasicBlock, ArrayList<BasicBlock>> preMap; // 前驱基本块
    private HashMap<BasicBlock, ArrayList<BasicBlock>> sucMap; // 后继基本块
    // 直接支配映射：每个基本块直接支配哪些基本块
    private HashMap<BasicBlock, ArrayList<BasicBlock>> immediateDominatesMap;
    private int activeCnt; // 活跃变量计数
    /**
     * 构造函数
     *
     * @param name       函数名
     * @param returnType 返回类型
     */
    public Function(String name, LLVMType returnType) {
        super(name, LLVMType.funcType); // 使用函数类型
        this.returnType = Objects.requireNonNull(returnType, "Return type cannot be null");
        this.params = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
    }

    // 变量ID管理
    public int getVarId() {
        varId++;
        return varId - 1;
    }
    public void setActiveCnt(int activeCnt) {
        this.activeCnt = activeCnt;
    }
    public int getActiveCnt() {
        return activeCnt;
    }

    public void setVarId(int varId) {
        this.varId = varId;
    }

    // 基本块ID管理
    public int getBlockId() {
        blockId++;
        return blockId - 1;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    /**
     * 添加参数到参数列表
     *
     * @param param 参数对象
     */
    public void addParam(Param param) {
        Objects.requireNonNull(param, "Param cannot be null");
        if (!params.contains(param)) { // 避免重复添加
            params.add(param);
        }
    }

    /**
     * 获取参数列表
     *
     * @return 参数列表
     */
    public List<Param> getParams() {
        return new ArrayList<>(params); // 返回副本，防止直接修改
    }

    /**
     * 添加基本块到函数
     *
     * @param block 基本块对象
     */
    public void addBasicBlock(BasicBlock block) {
        Objects.requireNonNull(block, "BasicBlock cannot be null");
        if (!basicBlocks.contains(block)) { // 避免重复添加
            basicBlocks.add(block);
        }
    }

    /**
     * 获取基本块列表
     *
     * @return 基本块列表
     */
    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    /**
     * 获取函数返回类型
     *
     * @return 返回类型
     */
    public LLVMType getReturnType() {
        return returnType;
    }

    /**
     * 获取寄存器池
     *
     * @return 寄存器池
     */
    public HashMap<Value, Register> getRegisterPool() {
        return registerPool;
    }

    /**
     * 设置寄存器池
     *
     * @param registerPool 寄存器池
     */
    public void setRegisterPool(HashMap<Value, Register> registerPool) {
        this.registerPool = registerPool;
    }

    /**
     * 返回LLVM IR格式字符串
     *
     * @return 格式化的函数定义
     */
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("define dso_local ");

        // 动态获取返回类型
        if(returnType.isVoid()){
            ret.append("void ");
        }else if(returnType.isInt8()){
            ret.append("i8 ");
        }else if(returnType.isInt32()){
            ret.append("i32 ");
        }
        ret.append(Name).append("(");

        // 构建参数列表
        for (int i = 0; i < params.size(); i++) {
            ret.append(params.get(i));
            if (i < params.size() - 1) {
                ret.append(", ");
            }
        }

        ret.append(") {\n");

        // 构建基本块列表
        for (BasicBlock block : basicBlocks) {
            ret.append(block).append("\n");
        }

        ret.append("}");
        return ret.toString();
    }

    public void setPreMap(HashMap<BasicBlock, ArrayList<BasicBlock>> preMap) {
        this.preMap = preMap;
    }

    public void setSucMap(HashMap<BasicBlock, ArrayList<BasicBlock>> sucMap) {
        this.sucMap = sucMap;
    }

    public HashMap<BasicBlock, ArrayList<BasicBlock>> getPreMap() {
        return preMap;
    }

    public HashMap<BasicBlock, ArrayList<BasicBlock>> getSucMap() {
        return sucMap;
    }

    // 获取直接支配映射
    public HashMap<BasicBlock, ArrayList<BasicBlock>> getImmediateDominatesMap() {
        return immediateDominatesMap;
    }
    /**
     * 设置直接支配映射
     *
     * @param immediateDominatesMap 直接支配映射
     */
    public void setImdom(HashMap<BasicBlock, ArrayList<BasicBlock>> immediateDominatesMap) {
        this.immediateDominatesMap = immediateDominatesMap;
    }

    public String getVarName() {
        return IRBuilder.tempName + getVarId();
    }
    private Boolean gvnAble = null;
    public boolean isGvnAble() {
        if (gvnAble != null) {
            return gvnAble;
        }
        for (Param param : params) {
            if (param.getType().isPointer()) {
                gvnAble = false;
                return false;
            }
        }
        for (BasicBlock block : basicBlocks) {
            for (Instruction instr : block.getInstrs()) {
                if (instr instanceof Call
                        || instr instanceof Getint ||
                        instr instanceof Putint ||
                        instr instanceof Putstr ||
                instr instanceof Getchar ||
                instr instanceof Putch) {
                    gvnAble = false;
                    return false;

                }
                for (Value operand : instr.getOperands()) {
                    if ((operand instanceof GlobalVar && !((GlobalVar) operand).isConst())) {
                        gvnAble = false;
                        return false;
                    }
                }
            }
        }
        gvnAble = true;
        return true;
    }
    private HashSet<Function> call;
    public void setCall(HashSet<Function> call) {
        this.call = call;
    }
    public HashSet<Function> getCall() {
        return call;
    }
    private boolean hasSideEffects;
    public void setHasSideEffects(boolean hasSideEffects) {
        this.hasSideEffects = hasSideEffects;
    }
    public boolean isHasSideEffects() {
        return hasSideEffects;
    }

    private ArrayList<BasicBlock> postOrder;

    public ArrayList<BasicBlock> getPostOrderForIdomTree() {
        postOrder = new ArrayList<>();
        visitIdomTree(basicBlocks.get(0));
        return postOrder;
    }

    public void visitIdomTree(BasicBlock curBlock) {
        for (BasicBlock idommed : curBlock.getImdom()) {
            visitIdomTree(idommed);
        }
        postOrder.add(curBlock);
    }
}
