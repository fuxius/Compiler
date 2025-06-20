package midEnd.Loop;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.Base.Core.Value;
import LLVMIR.Global.Function;
import LLVMIR.Global.GlobalVar;
import LLVMIR.Base.Core.Module;
import LLVMIR.Ins.Call;
import LLVMIR.Ins.Mem.Load;
import LLVMIR.Ins.Mem.Store;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 内存访问优化器
 * 主要实现两种优化：
 * 1. 全局变量本地化：将全局变量的访问转换为本地变量，减少内存访问
 * 2. 冗余加载消除：合并短时间内对同一地址的重复加载
 */
public class MemoryAccessOptimize {

    /**
     * 对整个模块进行内存访问优化
     * @param module 待优化的LLVM IR模块
     */
    public static void optimize(Module module) {
        for (Function function : module.getFunctions()) {
            for (BasicBlock block : function.getBasicBlocks()) {
                optimizeGlobalVarAccess(block);
                eliminateRedundantLoads(block);
            }
        }
    }

    /**
     * 优化基本块中的全局变量访问
     * 将全局变量的读写转换为本地变量操作，仅在必要时写回内存
     * @param block 待优化的基本块
     */
    private static void optimizeGlobalVarAccess(BasicBlock block) {
        ArrayList<Instruction> instructions = new ArrayList<>(block.getInstrs());

        // 维护全局变量的本地缓存
        HashMap<Value, Value> globalVarCache = new HashMap<>();
        // 记录需要写回的全局变量
        HashMap<Value, Value> pendingWrites = new HashMap<>();

        for (Instruction instruction : instructions) {
            if (instruction instanceof Load) {
                handleLoadInstruction((Load) instruction, block, globalVarCache);
            } else if (instruction instanceof Store) {
                handleStoreInstruction((Store) instruction, block, globalVarCache, pendingWrites);
            } else if (instruction instanceof Call) {
                // 函数调用可能修改全局变量，需要写回并清空缓存
                handleCallInstruction((Call) instruction, block, globalVarCache, pendingWrites);
            }
        }

        // 在基本块结束前写回所有待写入的全局变量
        writeBackGlobalVars(block, pendingWrites);
    }

    /**
     * 处理加载指令的优化
     */
    private static void handleLoadInstruction(Load load, BasicBlock block,
                                              HashMap<Value, Value> globalVarCache) {
        Value pointer = load.getOperands().get(0);
        if (pointer instanceof GlobalVar) {
            if (globalVarCache.containsKey(pointer)) {
                // 如果全局变量已缓存，直接使用缓存值
                load.modifyValueForUsers(globalVarCache.get(pointer));
                load.removeOperands();
                block.getInstrs().remove(load);
            } else {
                // 否则缓存当前加载的值
                globalVarCache.put(pointer, load);
            }
        }
    }

    /**
     * 处理存储指令的优化
     */
    private static void handleStoreInstruction(Store store, BasicBlock block,
                                               HashMap<Value, Value> globalVarCache,
                                               HashMap<Value, Value> pendingWrites) {
        Value pointer = store.getTo();
        if (pointer instanceof GlobalVar) {
            Value storedValue = store.getFrom();
            globalVarCache.put(pointer, storedValue);
            pendingWrites.put(pointer, storedValue);
            store.removeOperands();
            block.getInstrs().remove(store);
        }
    }

    /**
     * 处理函数调用指令
     */
    private static void handleCallInstruction(Call call, BasicBlock block,
                                              HashMap<Value, Value> globalVarCache,
                                              HashMap<Value, Value> pendingWrites) {
        int insertPosition = block.getInstrs().indexOf(call);
        // 在函数调用前写回所有待写入的全局变量
        pendingWrites.forEach((globalVar, value) -> {
            Store store = new Store(value, globalVar, block);
            block.getInstrs().add(insertPosition, store);
        });

        globalVarCache.clear();
        pendingWrites.clear();
    }

    /**
     * 将待写入的全局变量写回内存
     */
    private static void writeBackGlobalVars(BasicBlock block,
                                            HashMap<Value, Value> pendingWrites) {
        pendingWrites.forEach((globalVar, value) -> {
            Store store = new Store(value, globalVar, block);
            block.getInstrs().add(block.getInstrs().size() - 1, store);
        });
    }

    /**
     * 消除基本块中的冗余加载指令
     * 合并对同一内存地址的重复加载
     * @param block 待优化的基本块
     */
    public static void eliminateRedundantLoads(BasicBlock block) {
        ArrayList<Instruction> instructions = new ArrayList<>(block.getInstrs());
        // 记录地址到最近一次加载值的映射
        HashMap<Value, Value> addressValueMap = new HashMap<>();

        for (Instruction instruction : instructions) {
            if (instruction instanceof Load) {
                processLoadInstruction((Load) instruction, block, addressValueMap);
            } else if (instruction instanceof Store) {
                processStoreInstruction((Store) instruction, addressValueMap);
            } else if (instruction instanceof Call) {
                // 函数调用可能修改任何内存位置，清空缓存
                addressValueMap.clear();
            }
        }
    }

    /**
     * 处理加载指令的冗余消除
     */
    private static void processLoadInstruction(Load load, BasicBlock block,
                                               HashMap<Value, Value> addressValueMap) {
        Value address = load.getOperands().get(0);
        if (addressValueMap.containsKey(address)) {
            // 发现重复加载，使用之前加载的值
            load.modifyValueForUsers(addressValueMap.get(address));
            load.removeOperands();
            block.getInstrs().remove(load);
        } else {
            // 记录新的加载
            addressValueMap.put(address, load);
        }
    }

    /**
     * 处理存储指令的影响
     */
    private static void processStoreInstruction(Store store,
                                                HashMap<Value, Value> addressValueMap) {
        // 存储指令会改变内存内容，更新映射
        addressValueMap.clear();
        addressValueMap.put(store.getTo(), store.getFrom());
    }
}

//这个内存访问优化器实现了两种重要的内存优化：
//
//全局变量本地化优化：
//
//
//基本思路：
//
//将全局变量的访问转换为本地变量操作
//维护全局变量的本地缓存
//必要时才写回内存
//
//
//具体策略：
//
//遇到Load：优先使用缓存值，避免内存读取
//遇到Store：更新缓存，加入待写回队列
//遇到函数调用：写回所有待写全局变量并清空缓存
//基本块结束：写回所有待写变量
//
//
//
//
//冗余加载消除优化：
//
//
//基本思路：
//
//识别并合并短时间内对同一地址的重复加载
//使用已加载的值代替重复加载
//
//
//具体策略：
//
//维护地址到值的映射表
//遇到Load：检查是否存在最近的加载
//遇到Store：更新地址对应的最新值
//遇到函数调用：清空映射表(保守处理)