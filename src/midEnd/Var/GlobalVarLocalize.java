package midEnd.Var;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Constant;
import LLVMIR.Base.Core.User;
import LLVMIR.Global.Function;
import LLVMIR.Global.GlobalVar;
import LLVMIR.IRBuilder;
import LLVMIR.Ins.Mem.Alloca;
import LLVMIR.Ins.Call;
import LLVMIR.Ins.Mem.Store;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.LLVMType.PointerType;
import LLVMIR.Base.Core.Module;
import LLVMIR.Base.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GlobalVarLocalize {
    // 全局变量使用映射：GlobalVar -> 使用该变量的函数集合
    private static HashMap<GlobalVar, HashSet<Function>> globalVarUsageMap;
    // 函数调用映射：被调用的函数 -> 调用该函数的函数集合
    private static HashMap<Function, HashSet<Function>> functionCallMap;

    /**
     * 主方法，执行全局变量的本地化处理
     * @param module LLVM IR 模块
     */
    public static void globalVarLocalize(Module module) {
        globalVarUsageMap = new HashMap<>();
        functionCallMap = new HashMap<>();
        analyzeGlobalVarUsage(module);
        buildFunctionCallMap(module);
        performLocalization(module);
    }

    /**
     * 分析全局变量的使用情况，构建 globalVarUsageMap
     * @param module LLVM IR 模块
     */
    public static void analyzeGlobalVarUsage(Module module) {
        for (GlobalVar globalVar : module.getGlobalVars()) {
            for (User user : globalVar.getUsers()) {
                Instruction userInstruction = (Instruction) user;
                Function parentFunction = userInstruction.getParentBlock().getParentFunc();
                // 初始化全局变量对应的函数集合
                globalVarUsageMap.computeIfAbsent(globalVar, k -> new HashSet<>()).add(parentFunction);
            }
        }
    }

    /**
     * 构建函数调用映射，生成 functionCallMap
     * @param module LLVM IR 模块
     */
    public static void buildFunctionCallMap(Module module) {
        for (Function callerFunction : module.getFunctions()) {
            for (BasicBlock block : callerFunction.getBasicBlocks()) {
                for (Instruction instr : block.getInstrs()) {
                    if (instr instanceof Call) {
                        Function calleeFunction = (Function) instr.getOperands().get(0);
                        // 初始化被调用函数对应的调用者集合
                        functionCallMap.computeIfAbsent(calleeFunction, k -> new HashSet<>()).add(callerFunction);
                    }
                }
            }
        }
    }

    /**
     * 执行全局变量的本地化处理
     * @param module LLVM IR 模块
     */
    public static void performLocalization(Module module) {
        // 创建全局变量的副本以避免在迭代过程中修改集合
        ArrayList<GlobalVar> globalVarsCopy = new ArrayList<>(module.getGlobalVars());
        for (GlobalVar globalVar : globalVarsCopy) {
            // 如果全局变量未被使用，则从模块中移除
            if (!globalVarUsageMap.containsKey(globalVar)) {
                module.getGlobalVars().remove(globalVar);
            }
            // 如果全局变量仅在一个函数中使用
            else if (globalVarUsageMap.get(globalVar).size() == 1) {
                Function singleUsageFunction = globalVarUsageMap.get(globalVar).iterator().next();
                // 如果函数的活跃计数超过阈值，则跳过本地化
                if (singleUsageFunction.getActiveCnt() > 18) {
                    continue;
                }
                // 如果函数未被调用且全局变量的类型为 int32 指针
                if (!functionCallMap.containsKey(singleUsageFunction) &&
                        (((PointerType) (globalVar.getType())).getPointedType().isInt32()||((PointerType) (globalVar.getType())).getPointedType().isInt8())) {
                    BasicBlock entryBlock = singleUsageFunction.getBasicBlocks().get(0);
                    // 在入口块创建 Alloca 指令分配局部变量
                    Alloca allocaInstr = createAllocaInstruction(singleUsageFunction, entryBlock);
                    entryBlock.getInstrs().add(0, allocaInstr);
                    // 创建 Store 指令初始化局部变量
                    Store storeInstr = createStoreInstruction(globalVar, allocaInstr, entryBlock);
                    entryBlock.getInstrs().add(1, storeInstr);
                    // 修改全局变量的用户为新分配的局部变量
                    globalVar.modifyValueForUsers(allocaInstr);
                    // 从模块中移除全局变量
                    module.getGlobalVars().remove(globalVar);
                }
            }
        }
    }

    /**
     * 创建 Alloca 指令用于分配局部变量
     * @param function 当前函数
     * @param block 当前基本块
     * @return 创建的 Alloca 指令
     */
    private static Alloca createAllocaInstruction(Function function, BasicBlock block) {
        String allocaName = IRBuilder.tempName + function.getVarId();
        return new Alloca(allocaName, block, LLVMType.Int32);
    }

    /**
     * 创建 Store 指令用于初始化局部变量
     * @param globalVar 全局变量
     * @param allocaInstr 对应的 Alloca 指令
     * @param block 当前基本块
     * @return 创建的 Store 指令
     */
    private static Store createStoreInstruction(GlobalVar globalVar, Alloca allocaInstr, BasicBlock block) {
        int initialValue = globalVar.isZeroInitial() ? 0 : globalVar.getInitial().get(0);
        Constant initialConst = new Constant(initialValue);
        return new Store(initialConst, allocaInstr, block);
    }
}
