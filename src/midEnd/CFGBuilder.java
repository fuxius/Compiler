package midEnd;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;
import LLVMIR.Ins.Branch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * 控制流图构建器
 */
public class CFGBuilder {
    // 基本块到其后继基本块的映射
    private static HashMap<BasicBlock, ArrayList<BasicBlock>> sucMap;
    // 基本块到其前驱基本块的映射
    private static HashMap<BasicBlock, ArrayList<BasicBlock>> preMap;
    // 基本块到其支配者基本块的映射
    private static HashMap<BasicBlock, ArrayList<BasicBlock>> dominatedByMap;
    // 基本块到其支配的基本块的映射
    private static HashMap<BasicBlock, ArrayList<BasicBlock>> dominatesMap;
    // 基本块到其直接支配者基本块的映射
    private static HashMap<BasicBlock, BasicBlock> immediateDominatorMap;
    // 基本块到其直接支配的基本块的映射
    private static HashMap<BasicBlock, ArrayList<BasicBlock>> immediateDominatesMap;
    // 当前处理的函数
    private static Function currentFunction;

    /**
     * 构建模块中所有函数的控制流图（CFG）、支配树和支配边界
     *
     * @param module 要处理的模块
     */
    public static void buildCFG(Module module) {
        for (Function func : module.getFunctions()) { // 遍历模块中的每个函数
            initializeMaps(); // 初始化映射关系
            initializeBasicBlocks(func); // 初始化每个基本块的映射列表

            currentFunction = func; // 设置当前处理的函数
            buildControlFlowGraph(func); // 构建控制流图
            printControlFlowGraph(func); // 打印控制流图
            buildDominatorTree(); // 构建支配树
            buildDominanceFrontier(); // 构建支配边界

            // 设置函数的直接支配映射
            currentFunction.setImdom(immediateDominatesMap);

            // 打印支配相关信息
            printDominators();
            printImmediateDominators();
            printDominanceFrontier();
        }
    }

    /**
     * 初始化所有映射关系
     */
    private static void initializeMaps() {
        sucMap = new HashMap<>();
        preMap = new HashMap<>();
        dominatesMap = new HashMap<>();
        dominatedByMap = new HashMap<>();
        immediateDominatorMap = new HashMap<>();
        immediateDominatesMap = new HashMap<>();
    }

    /**
     * 初始化每个基本块的映射列表
     *
     * @param func 当前函数
     */
    private static void initializeBasicBlocks(Function func) {
        for (BasicBlock block : func.getBasicBlocks()) {
            sucMap.put(block, new ArrayList<>());
            preMap.put(block, new ArrayList<>());
            dominatesMap.put(block, new ArrayList<>());
            dominatedByMap.put(block, new ArrayList<>());
            immediateDominatesMap.put(block, new ArrayList<>());
        }
    }

    /**
     * 构建当前函数的控制流图
     *
     * @param func 要构建控制流图的函数
     */
    private static void buildControlFlowGraph(Function func) {
        preMap.clear(); // 清空前驱映射
        sucMap.clear(); // 清空后继映射

        // 初始化每个基本块的前驱和后继列表
        for (BasicBlock block : func.getBasicBlocks()) {
            preMap.put(block, new ArrayList<>()); // 初始化前驱列表
            sucMap.put(block, new ArrayList<>()); // 初始化后继列表
        }

        // 遍历每个基本块，分析最后一条指令以构建前驱和后继关系
        for (BasicBlock block : func.getBasicBlocks()) {
            Instruction lastInstr = block.getInstrs().get(block.getInstrs().size() - 1); // 获取基本块的最后一条指令
            if (lastInstr instanceof Branch) { // 如果是分支指令
                Branch branch = (Branch) lastInstr;
                if (branch.isConditional()) { // 条件分支
                    BasicBlock thenBlock = branch.getThenBlock(); // 获取then分支目标基本块
                    BasicBlock elseBlock = branch.getElseBlock(); // 获取else分支目标基本块
                    addSuccessor(block, thenBlock);
                    addSuccessor(block, elseBlock);
                } else { // 无条件分支
                    BasicBlock targetBlock = branch.getTargetBlock(); // 获取跳转目标基本块
                    addSuccessor(block, targetBlock);
                }
            }
        }

        // 设置函数的前驱和后继映射
        func.setPreMap(preMap);
        func.setSucMap(sucMap);

        // 设置每个基本块的前驱和后继列表
        for (BasicBlock block : func.getBasicBlocks()) {
            block.setPredecessors(preMap.get(block));
            block.setSuccessors(sucMap.get(block));
        }
    }

    /**
     * 添加后继基本块并更新前驱映射
     *
     * @param block      当前基本块
     * @param successor  后继基本块
     */
    private static void addSuccessor(BasicBlock block, BasicBlock successor) {
        sucMap.get(block).add(successor); // 添加后继
        preMap.get(successor).add(block); // 添加前驱
    }

    /**
     * 构建当前函数的支配树
     */
    private static void buildDominatorTree() {
        BasicBlock entryBlock = getEntryBlock(); // 获取入口基本块
        establishDominanceRelations(entryBlock); // 建立支配关系
        determineImmediateDominators(); // 确定直接支配者
        currentFunction.setImdom(immediateDominatesMap); // 设置函数的直接支配映射
        assignImmediateDominates(); // 分配直接支配的基本块
        computeImmediateDominatorTreeDepth(getEntryBlock(), 0); // 计算直接支配树的深度
    }

    /**
     * 获取当前函数的入口基本块
     *
     * @return 入口基本块
     */
    private static BasicBlock getEntryBlock() {
        return currentFunction.getBasicBlocks().get(0);
    }

    /**
     * 建立支配关系
     *
     * @param entryBlock 入口基本块
     */
    private static void establishDominanceRelations(BasicBlock entryBlock) {
        for (BasicBlock potentialDominator : currentFunction.getBasicBlocks()) { // 遍历所有基本块
            HashSet<BasicBlock> reachableBlocks = new HashSet<>(); // 初始化可达基本块集合
            findReachableBlocks(entryBlock, potentialDominator, reachableBlocks); // 查找可达基本块
            updateDominanceMaps(potentialDominator, reachableBlocks); // 更新支配映射
        }
    }

    /**
     * 更新支配映射
     *
     * @param dominator        支配者基本块
     * @param reachableBlocks 可达基本块集合
     */
    private static void updateDominanceMaps(BasicBlock dominator, HashSet<BasicBlock> reachableBlocks) {
        for (BasicBlock block : currentFunction.getBasicBlocks()) { // 遍历所有基本块
            if (!reachableBlocks.contains(block)) { // 如果基本块不在可达集合中
                dominatesMap.get(dominator).add(block); // 将基本块加入支配的基本块列表
                dominatedByMap.get(block).add(dominator); // 将支配者加入被支配的基本块列表
            }
        }
        dominator.setDom(dominatesMap.get(dominator)); // 设置基本块的支配列表
    }

    /**
     * 确定每个基本块的直接支配者
     */
    private static void determineImmediateDominators() {
        for (BasicBlock block : currentFunction.getBasicBlocks()) { // 遍历所有基本块
            for (BasicBlock dominator : dominatedByMap.get(block)) { // 遍历被支配基本块的支配者列表
                if (isImmediateDominator(block, dominator)) { // 检查是否为直接支配者
                    block.setImdommedBy(dominator); // 设置直接支配者
                    immediateDominatorMap.put(block, dominator); // 更新直接支配者映射
                    immediateDominatesMap.get(dominator).add(block); // 将被支配基本块加入直接支配的列表
                    break; // 找到直接支配者后退出循环
                }
            }
        }
    }

    /**
     * 检查一个支配者是否为另一个基本块的直接支配者
     *
     * @param block     被支配的基本块
     * @param dominator 潜在的直接支配者基本块
     * @return 如果是直接支配者，返回 true；否则返回 false
     */
    private static boolean isImmediateDominator(BasicBlock block, BasicBlock dominator) {
        for (BasicBlock otherDominator : dominatedByMap.get(block)) { // 遍历其他支配者
            if (dominatesMap.get(dominator).contains(otherDominator)) { // 如果其他支配者被当前支配者支配
                return false; // 不是直接支配者
            }
        }
        return true; // 是直接支配者
    }

    /**
     * 分配直接支配的基本块
     */
    private static void assignImmediateDominates() {
        currentFunction.setImdom(immediateDominatesMap);
        for (BasicBlock block : currentFunction.getBasicBlocks()) { // 遍历所有基本块
            block.setImdom(immediateDominatesMap.get(block)); // 设置基本块的直接支配列表
        }
    }

    /**
     * 递归计算直接支配树的深度
     *
     * @param block 当前基本块
     * @param depth 当前深度
     */
    private static void computeImmediateDominatorTreeDepth(BasicBlock block, int depth) {
        block.setImdomDepth(depth); // 设置基本块的直接支配深度
        for (BasicBlock immediateDominated : block.getImdom()) { // 遍历直接支配的基本块
            computeImmediateDominatorTreeDepth(immediateDominated, depth + 1); // 递归计算深度
        }
    }

    /**
     * 查找从当前基本块到支配者基本块的可达基本块
     *
     * @param currentBlock 当前遍历的基本块
     * @param dominator    支配者基本块
     * @param reachable    已访问的可达基本块集合
     */
    private static void findReachableBlocks(BasicBlock currentBlock, BasicBlock dominator, HashSet<BasicBlock> reachable) {
        reachable.add(currentBlock); // 将当前基本块加入可达集合
        if (currentBlock == dominator) { // 如果到达支配者基本块
            return; // 结束递归
        }
        for (BasicBlock child : sucMap.get(currentBlock)) { // 遍历当前基本块的后继基本块
            if (!reachable.contains(child)) { // 如果后继基本块未被访问
                findReachableBlocks(child, dominator, reachable); // 递归查找
            }
        }
    }

    /**
     * 构建当前函数的支配边界
     */
    private static void buildDominanceFrontier() {
        for (BasicBlock dominator : currentFunction.getBasicBlocks()) { // 遍历所有基本块作为支配者
            ArrayList<BasicBlock> dominanceFrontier = new ArrayList<>(); // 初始化支配边界列表
            computeDominanceFrontierForDominator(dominator, dominanceFrontier); // 计算支配边界
            dominator.setDF(dominanceFrontier); // 设置基本块的支配边界列表
        }
    }

    /**
     * 计算给定支配者的支配边界
     *
     * @param dominator          支配者基本块
     * @param dominanceFrontier  支配边界列表
     */
    private static void computeDominanceFrontierForDominator(BasicBlock dominator, ArrayList<BasicBlock> dominanceFrontier) {
        for (BasicBlock dominated : dominatesMap.get(dominator)) { // 遍历支配者支配的基本块
            for (BasicBlock child : sucMap.get(dominated)) { // 遍历被支配基本块的后继基本块
                if (!dominatesMap.get(dominator).contains(child) && !dominanceFrontier.contains(child)) { // 如果后继基本块不被支配且未在支配边界中
                    dominanceFrontier.add(child); // 添加到支配边界列表
                }
            }
        }
        for (BasicBlock child : sucMap.get(dominator)) { // 遍历支配者的后继基本块
            if (!dominatesMap.get(dominator).contains(child) && !dominanceFrontier.contains(child)) { // 如果后继基本块不被支配且未在支配边界中
                dominanceFrontier.add(child); // 添加到支配边界列表
            }
        }
    }

    /**
     * 打印当前函数的支配者信息
     */
    private static void printDominators() {
        System.out.println("支配者信息:");
        for (BasicBlock block : currentFunction.getBasicBlocks()) {
            System.out.print("基本块: " + block.getName() + "  支配者: ");
            ArrayList<BasicBlock> dominators = dominatesMap.get(block);
            if (dominators != null) {
                for (BasicBlock dominator : dominators) {
                    System.out.print(dominator.getName() + " ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * 打印当前函数的直接支配者信息
     */
    private static void printImmediateDominators() {
        System.out.println("直接支配者信息:");
        for (BasicBlock block : currentFunction.getBasicBlocks()) {
            BasicBlock immediateDominator = immediateDominatorMap.get(block);
            System.out.println("基本块: " + block.getName() + "  直接支配者: " + (immediateDominator != null ? immediateDominator.getName() : "无"));
        }
        System.out.println();
    }

    /**
     * 打印当前函数的支配边界信息
     */
    private static void printDominanceFrontier() {
        System.out.println("支配边界信息:");
        for (BasicBlock block : currentFunction.getBasicBlocks()) {
            System.out.print("基本块: " + block.getName() + "  支配边界: ");
            List<BasicBlock> dfList = block.getDF();
            if (dfList != null) {
                for (BasicBlock dfBlock : dfList) {
                    System.out.print(dfBlock.getName() + " ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * 打印当前函数的控制流图信息
     *
     * @param function 要打印控制流图的函数
     */
    private static void printControlFlowGraph(Function function) {
        System.out.println("函数: " + function.getName()); // 打印函数名称
        for (BasicBlock bb : function.getBasicBlocks()) { // 遍历函数中的所有基本块
            System.out.print("基本块: " + bb.getName() + "\n  前驱: "); // 打印基本块名称和前驱
            ArrayList<BasicBlock> predecessors = preMap.get(bb);
            if (predecessors == null || predecessors.isEmpty()) { // 如果前驱为空
                System.out.print("无 ");
            } else {
                for (BasicBlock pred : predecessors) { // 遍历前驱基本块
                    System.out.print(pred.getName() + " "); // 打印前驱基本块名称
                }
            }

            System.out.print("\n  后继: "); // 打印后继
            ArrayList<BasicBlock> successors = sucMap.get(bb);
            if (successors == null || successors.isEmpty()) { // 如果后继为空
                System.out.print("无 ");
            } else {
                for (BasicBlock succ : successors) { // 遍历后继基本块
                    System.out.print(succ.getName() + " "); // 打印后继基本块名称
                }
            }
            System.out.println("\n"); // 换行
        }
    }
}
