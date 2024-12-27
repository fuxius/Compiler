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
//这个全局变量本地化优化器的主要思路是：
//将仅在单个函数中使用的全局变量转换为局部变量，以减少全局变量的数量和提高访问效率。具体步骤为：
//
//分析阶段：
//
//
//构建全局变量使用映射(globalVarUsageMap)：
//
//记录每个全局变量被哪些函数使用
//通过分析全局变量的用户来建立映射关系
//
//
//构建函数调用映射(functionCallMap)：
//
//记录函数间的调用关系
//分析哪些函数被其他函数调用
//
//
//
//
//本地化处理阶段：
//
//
//对每个全局变量进行评估：
//
//如果变量完全未使用，直接从模块移除
//如果变量仅在一个函数中使用，考虑进行本地化
//
//
//本地化条件：
//
//变量仅在单一函数中使用
//使用该变量的函数活跃变量计数不超过阈值(18)
//使用该变量的函数未被其他函数调用
//变量类型为int32或int8指针
//
//
//本地化转换：
//
//在函数入口块创建Alloca指令分配局部存储
//添加Store指令初始化局部变量
//将全局变量的所有使用替换为新的局部变量
//从模块中移除原全局变量
//
//
//
//这个优化的好处：
//
//减少全局变量的数量，降低命名空间污染
//改善数据局部性，提高访问速度
//简化变量生命周期管理
//减少潜在的并发访问问题
//为其他优化创造更多机会
//
//总的来说，这是一个通过将全局作用域变量转换为局部作用域变量来提高程序性能的优化。