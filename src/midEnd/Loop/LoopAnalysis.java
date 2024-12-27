package midEnd.Loop;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Core.Module;
import LLVMIR.Global.Function;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * 循环分析器
 * 负责识别和分析程序中的循环结构，包括：
 * 1. 循环入口识别
 * 2. 循环嵌套关系分析
 * 3. 循环深度计算
 */
public class LoopAnalysis {
    // 用于深度优先遍历时记录已访问的基本块
    private static HashSet<BasicBlock> visitedBlocks;

    /**
     * 对整个模块进行循环分析
     * @param module 待分析的LLVM IR模块
     */
    public static void analyzeLoop(Module module) {
        for (Function function : module.getFunctions()) {
            // 初始化所有基本块的循环信息
            initializeLoopInfo(function);
            // 对每个函数进行循环分析
            analyzeLoopsInFunction(function);
        }
    }

    /**
     * 初始化函数中所有基本块的循环信息
     */
    private static void initializeLoopInfo(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            block.setParentLoop(null);
        }
    }

    /**
     * 分析函数中的循环结构
     * @param function 待分析的函数
     */
    public static void analyzeLoopsInFunction(Function function) {
        // 获取支配树的后序遍历序列
        ArrayList<BasicBlock> postOrderBlocks = function.getPostOrderForIdomTree();

        // 识别所有循环结构
        identifyLoops(postOrderBlocks);

        // 分析循环的嵌套关系和深度
        visitedBlocks = new HashSet<>();
        analyzeLoopNesting(function.getBasicBlocks().get(0));
    }

    /**
     * 识别所有循环结构
     * @param postOrderBlocks 后序遍历的基本块序列
     */
    private static void identifyLoops(ArrayList<BasicBlock> postOrderBlocks) {
        for (BasicBlock headerBlock : postOrderBlocks) {
            ArrayList<BasicBlock> loopBackEdges = findBackEdges(headerBlock);
            if (!loopBackEdges.isEmpty()) {
                LoopInfo loop = new LoopInfo(headerBlock, loopBackEdges);
                buildLoopBody(loop, loopBackEdges);
            }
        }
    }

    /**
     * 查找循环的回边（后继节点支配其前驱节点的边）
     * @param headerBlock 可能的循环头节点
     * @return 回边的集合
     */
    private static ArrayList<BasicBlock> findBackEdges(BasicBlock headerBlock) {
        ArrayList<BasicBlock> backEdges = new ArrayList<>();
        for (BasicBlock predecessor : headerBlock.getPredecessors()) {
            // 如果前驱节点被循环头支配，说明找到了一个回边
            if (headerBlock.getDominators().contains(predecessor)) {
                backEdges.add(predecessor);
            }
        }
        return backEdges;
    }

    /**
     * 构建循环体
     * 使用广度优先搜索确定循环中的所有基本块
     */
    public static void buildLoopBody(LoopInfo loop, ArrayList<BasicBlock> loopBackEdges) {
        ArrayList<BasicBlock> workList = new ArrayList<>(loopBackEdges);
        while (!workList.isEmpty()) {
            BasicBlock currentBlock = workList.remove(0);
            processLoopBlock(currentBlock, loop, workList);
        }
    }

    /**
     * 处理循环中的一个基本块
     */
    private static void processLoopBlock(BasicBlock block, LoopInfo loop,
                                         ArrayList<BasicBlock> workList) {
        LoopInfo existingLoop = block.getParentLoop();

        if (existingLoop == null) {
            // 块不在任何循环中，直接添加到当前循环
            addBlockToLoop(block, loop, workList);
        } else {
            // 块已经在其他循环中，处理循环嵌套
            handleNestedLoop(existingLoop, loop, workList);
        }
    }

    /**
     * 将基本块添加到循环中
     */
    private static void addBlockToLoop(BasicBlock block, LoopInfo loop,
                                       ArrayList<BasicBlock> workList) {
        block.setParentLoop(loop);
        if (block != loop.getEntry()) {
            workList.addAll(block.getPredecessors());
        }
    }

    /**
     * 处理嵌套循环的情况
     */
    private static void handleNestedLoop(LoopInfo existingLoop, LoopInfo newLoop,
                                         ArrayList<BasicBlock> workList) {
        // 找到最外层的父循环
        LoopInfo outerMostLoop = findOuterMostLoop(existingLoop);

        if (outerMostLoop != newLoop) {
            outerMostLoop.setParentLoop(newLoop);
            // 处理外层循环入口的前驱
            for (BasicBlock predecessor : outerMostLoop.getEntry().getPredecessors()) {
                if (predecessor.getParentLoop() != outerMostLoop) {
                    workList.add(predecessor);
                }
            }
        }
    }

    /**
     * 找到最外层的父循环
     */
    private static LoopInfo findOuterMostLoop(LoopInfo loop) {
        LoopInfo outerLoop = loop;
        LoopInfo parent = loop.getParentLoop();
        while (parent != null) {
            outerLoop = parent;
            parent = parent.getParentLoop();
        }
        return outerLoop;
    }

    /**
     * 分析循环的嵌套关系和深度
     * 通过深度优先遍历计算每个循环的深度
     */
    private static void analyzeLoopNesting(BasicBlock block) {
        visitedBlocks.add(block);

        // 如果当前块是循环入口，计算循环深度
        LoopInfo loop = block.getParentLoop();
        if (loop != null && block == loop.getEntry()) {
            calculateLoopDepth(loop);
        }

        // 递归处理后继块
        for (BasicBlock successor : block.getSuccessors()) {
            if (!visitedBlocks.contains(successor)) {
                analyzeLoopNesting(successor);
            }
        }
    }

    /**
     * 计算循环的深度
     * 深度等于从当前循环到最外层循环的路径长度
     */
    private static void calculateLoopDepth(LoopInfo loop) {
        int depth = 1;
        LoopInfo parent = loop.getParentLoop();
        while (parent != null) {
            parent = parent.getParentLoop();
            depth++;
        }
        loop.setLoopDepth(depth);
    }
}

//这个循环分析器的主要思路是：
//识别、构建和分析程序中的循环结构，主要包含三个核心阶段：
//
//循环识别阶段：
//
//
//基于支配树的后序遍历来识别循环
//通过寻找回边(back edge)识别循环入口
//回边定义：如果一个边的目标节点支配其源节点，则该边为回边
//每个回边对应一个循环，其目标节点为循环的入口块
//
//
//循环体构建阶段：
//
//
//从回边的源节点开始，使用广度优先搜索
//向上遍历前驱节点，直到达到循环入口
//所有在这个过程中访问到的节点构成循环体
//处理嵌套循环的情况，确保正确建立循环间的包含关系
//
//
//循环嵌套分析阶段：
//
//
//分析循环之间的嵌套关系
//计算每个循环的深度
//深度定义为从当前循环到最外层循环的路径长度
//通过深度优先遍历保证分析的完整性