package midEnd.base;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Value;
import LLVMIR.Global.Function;

import java.util.HashSet;
import java.util.List;

/**
 * 活跃变量分析器
 */
public class ActiveVarAnalyzer {
    private static int maxActiveVariables = 0; // 记录最大活跃变量数量
    /**
     * 对指定函数进行活跃变量分析
     *
     * @param function 要分析的函数
     */
    public static void analyze(Function function) {
        List<BasicBlock> blocks = function.getBasicBlocks();

        // 第一步：计算每个基本块的 Def 和 Use 集合
        initializeDefUse(blocks);

        // 第二步：初始化每个基本块的 In 和 Out 集合为空集
        initializeInOutSets(blocks);

        // 第三步：迭代计算 In 和 Out 集合，直到收敛
        performLivenessAnalysis(blocks);

        // 将最大活跃变量数量设置到函数对象中
        function.setActiveCnt(maxActiveVariables);

        // 第四步：打印分析结果
        printLivenessResults(blocks);
    }

    /**
     * 计算每个基本块的 Def 和 Use 集合
     *
     * @param blocks 函数中的基本块列表
     */
    private static void initializeDefUse(List<BasicBlock> blocks) {
        for (BasicBlock bb : blocks) {
            bb.computeDefUse();
        }
    }

    /**
     * 初始化每个基本块的 In 和 Out 集合为空集
     *
     * @param blocks 函数中的基本块列表
     */
    private static void initializeInOutSets(List<BasicBlock> blocks) {
        for (BasicBlock bb : blocks) {
            bb.setInSet(new HashSet<>());
            bb.setOutSet(new HashSet<>());
        }
    }

    /**
     * 执行活跃变量分析，迭代计算 In 和 Out 集合
     *
     * @param blocks 函数中的基本块列表
     */
    private static void performLivenessAnalysis(List<BasicBlock> blocks) {
        boolean changed = true;
        while (changed) {
            changed = false;

            // 逆序遍历基本块以加速收敛
            for (int i = blocks.size() - 1; i >= 0; i--) {
                BasicBlock bb = blocks.get(i);

                // 保存当前的 In 和 Out 集合
                HashSet<Value> oldIn = new HashSet<>(bb.getInSet());
                HashSet<Value> oldOut = new HashSet<>(bb.getOutSet());

                // 计算新的 Out 集合
                HashSet<Value> newOut = computeOutSet(bb);
                bb.setOutSet(newOut);

                // 计算新的 In 集合
                HashSet<Value> newIn = computeInSet(bb, newOut);
                bb.setInSet(newIn);
                // 更新最大活跃变量数量
                updateMaxActiveVariables(newIn, newOut);
                // 检查是否有变化
                if (!oldIn.equals(newIn) || !oldOut.equals(newOut)) {
                    changed = true;
                }
            }
        }
    }
    private static void updateMaxActiveVariables(HashSet<Value> inSet, HashSet<Value> outSet) {
        // 计算当前基本块活跃变量的总数
        int currentActiveVariables = inSet.size() + outSet.size();

        // 更新最大值
        maxActiveVariables = Math.max(maxActiveVariables, currentActiveVariables);
    }
    /**
     * 计算一个基本块的 Out 集合
     *
     * @param bb 要计算 Out 集合的基本块
     * @return 新的 Out 集合
     */
    private static HashSet<Value> computeOutSet(BasicBlock bb) {
        HashSet<Value> outSet = new HashSet<>();
        for (BasicBlock succ : bb.getSuccessors()) {
            outSet.addAll(succ.getInSet());
        }
        return outSet;
    }

    /**
     * 计算一个基本块的 In 集合
     *
     * @param bb     要计算 In 集合的基本块
     * @param newOut 计算得到的 Out 集合
     * @return 新的 In 集合
     */
    private static HashSet<Value> computeInSet(BasicBlock bb, HashSet<Value> newOut) {
        HashSet<Value> inSet = new HashSet<>(bb.getUseSet());
        HashSet<Value> outMinusDef = new HashSet<>(newOut);
        outMinusDef.removeAll(bb.getDefSet());
        inSet.addAll(outMinusDef);
        return inSet;
    }

    /**
     * 打印活跃变量分析结果
     *
     * @param blocks 函数中的基本块列表
     */
    private static void printLivenessResults(List<BasicBlock> blocks) {
        System.out.println("活跃变量分析结果:");
        for (BasicBlock bb : blocks) {
            System.out.println("基本块: " + bb.getName());
            System.out.print("  In: ");
            for (Value var : bb.getInSet()) {
                System.out.print(var.getName() + " ");
            }
            System.out.println();
            System.out.print("  Out: ");
            for (Value var : bb.getOutSet()) {
                System.out.print(var.getName() + " ");
            }
            System.out.println("\n");
        }
    }
}
