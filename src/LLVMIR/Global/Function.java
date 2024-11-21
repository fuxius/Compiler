package LLVMIR.Global;

import LLVMIR.BasicBlock;
import LLVMIR.LLVMType.FuncType;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Param;
import LLVMIR.User;

import java.util.ArrayList;
import java.util.List;

public class Function extends User {
    private LLVMType returnType; // 函数的返回类型
    private List<Param> params;  // 函数的参数列表
    private List<BasicBlock> basicBlocks;  // 函数包含的基本块
    private int varId;//变量id
    private int blockId;//块id

    public int getVarId() {
        return varId;
    }

    public void setVarId(int varId) {
        this.varId = varId;
    }

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public Function(String name, LLVMType returnType) {
        super(name, LLVMType.funcType); // 函数类型
        this.returnType = returnType;
        this.params = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
    }

    public void addParam(Param param) {
        params.add(param);
    }

    public List<Param> getParams() {
        return params;
    }

    public void addBasicBlock(BasicBlock block) {
        basicBlocks.add(block);
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public LLVMType getReturnType() {
        return returnType;
    }
    public String toString() {
        StringBuilder ret = new StringBuilder("define dso_local ");
        if (returnType.isVoid()) {
            ret.append("void ");
        } else {
            ret.append("i32 ");
        }
        ret.append(Name).append("(");
        for (Param param : params) {
            if (params.indexOf(param) == 0) {
                ret.append(param);
            } else {
                ret.append(",").append(param);
            }
        }
        ret.append("){\n");
        for (BasicBlock block : basicBlocks) {
            ret.append(block);
        }
        ret.append("\n}");
        return ret.toString();
    }
}

