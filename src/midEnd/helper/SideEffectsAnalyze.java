package midEnd.helper;

import LLVMIR.Base.*;
import LLVMIR.Base.Core.Value;
import LLVMIR.Base.Core.Module;
import LLVMIR.Global.Function;
import LLVMIR.Global.GlobalVar;
import LLVMIR.Ins.*;
import LLVMIR.Ins.CIo.Getint;
import LLVMIR.Ins.CIo.Putint;
import LLVMIR.Ins.CIo.Putstr;
import LLVMIR.Ins.Mem.Store;

import java.util.HashSet;

/**
 * 函数副作用分析器
 * 分析函数是否有副作用（如修改全局变量、IO操作等）
 * 同时处理函数调用链上的副作用传播
 */
public class SideEffectsAnalyze {

    /**
     * 对模块中的所有函数进行副作用分析
     * @param module 待分析的LLVM IR模块
     */
    public static void analyzeSideEffects(Module module) {
        // 第一遍：分析每个函数的直接副作用
        analyzeFunctionDirectEffects(module);

        // 第二遍：通过调用关系传播副作用信息
        propagateSideEffects(module);
    }

    /**
     * 分析函数的直接副作用
     */
    private static void analyzeFunctionDirectEffects(Module module) {
        for (Function function : module.getFunctions()) {
            DirectEffectAnalyzer analyzer = new DirectEffectAnalyzer();
            analyzer.analyze(function);
        }
    }

    /**
     * 通过调用关系传播副作用信息
     */
    private static void propagateSideEffects(Module module) {
        boolean hasChanges;
        do {
            hasChanges = false;
            for (Function function : module.getFunctions()) {
                if (updateFunctionSideEffects(function)) {
                    hasChanges = true;
                }
            }
        } while (hasChanges);
    }

    /**
     * 更新函数的副作用状态
     * @return 如果函数的副作用状态发生改变则返回true
     */
    private static boolean updateFunctionSideEffects(Function function) {
        if (function.isHasSideEffects()) {
            return false;
        }

        for (Function calledFunction : function.getCall()) {
            if (calledFunction.isHasSideEffects()) {
                function.setHasSideEffects(true);
                return true;
            }
        }
        return false;
    }

    /**
     * 直接副作用分析器
     * 分析单个函数中的直接副作用
     */
    private static class DirectEffectAnalyzer {
        private boolean hasSideEffects;
        private HashSet<Function> calledFunctions;

        public void analyze(Function function) {
            hasSideEffects = false;
            calledFunctions = new HashSet<>();

            analyzeBlocks(function);

            // 设置分析结果
            function.setCall(calledFunctions);
            function.setHasSideEffects(hasSideEffects);
        }

        /**
         * 分析函数中的所有基本块
         */
        private void analyzeBlocks(Function function) {
            for (BasicBlock block : function.getBasicBlocks()) {
                if (analyzeInstructions(block)) {
                    break;
                }
            }
        }

        /**
         * 分析基本块中的指令
         * @return 如果发现副作用则返回true
         */
        private boolean analyzeInstructions(BasicBlock block) {
            for (Instruction instruction : block.getInstrs()) {
                if (analyzeInstruction(instruction)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 分析单条指令的副作用
         * @return 如果指令有副作用则返回true
         */
        private boolean analyzeInstruction(Instruction instruction) {
            if (instruction instanceof Call) {
                Function target = (Function) instruction.getOperands().get(0);
                calledFunctions.add(target);
                return false;
            }

            if (hasDirectSideEffect(instruction)) {
                hasSideEffects = true;
                return true;
            }

            if (instruction instanceof Store) {
                return checkStoreEffect((Store) instruction);
            }

            return false;
        }

        /**
         * 检查指令是否有直接的副作用（如IO操作）
         */
        private boolean hasDirectSideEffect(Instruction instruction) {
            return instruction instanceof Getint ||
                    instruction instanceof Putint ||
                    instruction instanceof Putstr;
        }

        /**
         * 检查存储指令的副作用
         */
        private boolean checkStoreEffect(Store store) {
            Value destination = store.getOperands().get(1);

            // 检查是否写入全局变量
            if (destination instanceof GlobalVar) {
                hasSideEffects = true;
                return true;
            }

            // 检查是否写入参数或全局变量的指针
            if (destination instanceof GetPtr) {
                GetPtr getPtr = (GetPtr) destination;
                Value base = getPtr.getOperands().get(0);
                if (base instanceof Param || base instanceof GlobalVar) {
                    hasSideEffects = true;
                    return true;
                }
            }

            return false;
        }
    }
}
//这个函数副作用分析器的主要思路是：
//通过两次遍历来分析函数的副作用，包括直接副作用和通过函数调用传播的间接副作用。
//
//直接副作用分析：
//
//
//遍历每个函数的指令寻找以下直接副作用：
//
//IO操作：如getint、putint、putstr等
//全局变量修改：store指令写入全局变量
//参数指针写入：通过getptr获取的参数或全局变量的指针进行写操作
//
//
//同时收集函数调用关系，记录每个函数调用了哪些其他函数
//
//
//副作用传播分析：
//
//
//从已知有副作用的函数开始，通过调用关系图进行传播
//如果函数A调用了有副作用的函数B，则A也被标记为有副作用
//重复这个传播过程直到没有新的函数被标记为有副作用
//
//这种分析的主要用途：
//
//帮助确定哪些函数调用可以被优化（如纯函数可以被常量折叠）
//确定哪些函数调用的顺序是否可以调整
//识别可以被并行化的函数
//帮助死代码删除优化确定哪些函数调用是必需的
//
//分析结果可以用于：
//
//函数内联决策
//代码移动优化
//循环优化
//并行化优化
//公共子表达式删除
//
//这是一个保守的分析，即如果不能确定函数无副作用，就将其标记为有副作用，以保证优化的安全性。