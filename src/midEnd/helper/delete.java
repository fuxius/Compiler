package midEnd.helper;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Core.Module;
import LLVMIR.Global.Function;
import LLVMIR.Base.Instruction;
import LLVMIR.Ins.Branch;
import LLVMIR.Ins.Mem.Phi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * 代码优化辅助类
 * 用于删除不可达代码块和合并基本块
 */
public class delete {
    // 存储可达的基本块集合
    private static HashSet<BasicBlock> accessibleBlocks;

    /**
     * 简化模块中的代码
     * 包括删除死代码和不可达块
     * @param targetModule 待处理的模块
     */
    public static void simplify(Module targetModule) {
        for (Function currentFunc : targetModule.getFunctions()) {
            // 第一步：清理每个基本块中的死代码
            for (BasicBlock currentBlock : currentFunc.getBasicBlocks()) {
                cleanDeadCode(currentBlock);
            }

            // 第二步：标记从入口块开始的所有可达块
            BasicBlock startBlock = currentFunc.getBasicBlocks().get(0);
            accessibleBlocks = new HashSet<>();
            markReachableBlocks(startBlock);

            // 第三步：清理不可达的基本块
            removeUnreachableBlocks(currentFunc);
        }
    }

    /**
     * 清理基本块中的死代码
     * @param targetBlock 待处理的基本块
     */
    public static void cleanDeadCode(BasicBlock targetBlock) {
        List<Instruction> instrList = targetBlock.getInstrs();
        int terminatorPos = findTerminatorPosition(instrList);

        // 删除终止指令后的所有指令
        removeInstructionsAfterTerminator(instrList, terminatorPos);
    }

    /**
     * 查找基本块中终止指令的位置
     * @param instructions 指令列表
     * @return 终止指令的位置
     */
    private static int findTerminatorPosition(List<Instruction> instructions) {
        int pos = 0;
        while (pos < instructions.size()) {
            Instruction.InstrType currentType = instructions.get(pos).getInstrType();
            if (isTerminator(currentType)) {
                break;
            }
            pos++;
        }
        return pos;
    }

    /**
     * 判断指令类型是否为终止指令
     * @param type 指令类型
     * @return 是否为终止指令
     */
    private static boolean isTerminator(Instruction.InstrType type) {
        return type == Instruction.InstrType.JUMP ||
                type == Instruction.InstrType.BRANCH ||
                type == Instruction.InstrType.RETURN;
    }

    /**
     * 删除终止指令后的所有指令
     * @param instructions 指令列表
     * @param terminatorPos 终止指令位置
     */
    private static void removeInstructionsAfterTerminator(List<Instruction> instructions, int terminatorPos) {
        int nextPos = terminatorPos + 1;
        while (nextPos < instructions.size()) {
            instructions.get(nextPos).removeOperands();
            instructions.remove(nextPos);
        }
    }

    /**
     * 标记所有可达的基本块
     * @param currentBlock 当前基本块
     */
    public static void markReachableBlocks(BasicBlock currentBlock) {
        if (accessibleBlocks.contains(currentBlock)) {
            return;
        }

        accessibleBlocks.add(currentBlock);

        // 获取块的最后一条指令
        List<Instruction> instrList = currentBlock.getInstrs();
        Instruction lastInstr = instrList.get(instrList.size() - 1);

        // 根据不同的分支类型处理后继块
        processSuccessorBlocks(lastInstr);
    }

    /**
     * 处理后继基本块
     * @param terminator 终止指令
     */
    private static void processSuccessorBlocks(Instruction terminator) {
        if (terminator instanceof Branch) {
            Branch branchInstr = (Branch) terminator;
            if (branchInstr.isConditional()) {
                // 条件分支：递归处理两个分支
                markReachableBlocks(branchInstr.getThenBlock());
                markReachableBlocks(branchInstr.getElseBlock());
            } else {
                // 无条件跳转：只处理目标块
                markReachableBlocks(branchInstr.getTargetBlock());
            }
        }
    }

    /**
     * 删除不可达的基本块
     * @param function 当前函数
     */
    private static void removeUnreachableBlocks(Function function) {
        Iterator<BasicBlock> blockIter = function.getBasicBlocks().iterator();
        while (blockIter.hasNext()) {
            BasicBlock currentBlock = blockIter.next();
            if (!accessibleBlocks.contains(currentBlock)) {
                cleanupBlock(currentBlock);
                blockIter.remove();
                currentBlock.setDeleted();
            }
        }
    }

    /**
     * 清理基本块的操作数
     * @param block 待清理的基本块
     */
    private static void cleanupBlock(BasicBlock block) {
        // 清理块内所有指令的操作数
        for (Instruction instr : block.getInstrs()) {
            instr.removeOperands();
        }
        // 清理块本身的操作数
        block.removeOperands();
    }

    /**
     * 合并基本块
     * 将只有一个后继且该后继只有一个前驱的基本块合并
     * @param targetModule 待处理的模块
     */
    public static void mergeBlocks(Module targetModule) {
        for (Function currentFunc : targetModule.getFunctions()) {
            processFunctionBlocks(currentFunc);
            // 移除已删除的基本块
            currentFunc.getBasicBlocks().removeIf(BasicBlock::isDeleted);
        }
    }

    /**
     * 处理函数中的基本块合并
     * @param function 当前函数
     */
    private static void processFunctionBlocks(Function function) {
        for (BasicBlock currentBlock : function.getBasicBlocks()) {
            if (canMergeBlock(currentBlock)) {
                BasicBlock nextBlock = currentBlock.getSuccessors().get(0);
                mergeConsecutiveBlocks(currentBlock, nextBlock);
            }
        }
    }

    /**
     * 检查基本块是否可以合并
     * @param block 待检查的基本块
     * @return 是否可以合并
     */
    private static boolean canMergeBlock(BasicBlock block) {
        if (block.isDeleted() || block.getSuccessors().size() != 1) {
            return false;
        }
        BasicBlock successor = block.getSuccessors().get(0);
        return successor.getPredecessors().size() == 1;
    }

    /**
     * 合并两个连续的基本块
     * @param source 源基本块
     * @param target 目标基本块
     */
    private static void mergeConsecutiveBlocks(BasicBlock source, BasicBlock target) {
        // 移除源块的跳转指令
        source.getInstrs().remove(source.getInstrs().size() - 1);

        // 处理目标块中的指令
        for (Instruction instr : target.getInstrs()) {
            if (instr instanceof Phi) {
                handlePhiInstruction((Phi) instr, source);
            } else {
                // 移动普通指令到源块
                source.addInstr(instr);
                instr.setParentBlock(source);
            }
        }

        // 更新引用关系并标记目标块为已删除
        target.modifyValueForUsers(source);
        target.setDeleted();
    }

    /**
     * 处理Phi指令
     * @param phi Phi指令
     * @param sourceBlock 源基本块
     */
    private static void handlePhiInstruction(Phi phi, BasicBlock sourceBlock) {
        int blockIndex = phi.getIncomingBlocks().indexOf(sourceBlock);
        phi.modifyValueForUsers(phi.getOperands().get(blockIndex));
        phi.removeOperands();
    }
}

//这个代码优化辅助类主要实现了两个重要的优化功能：死代码删除和基本块合并。主要思路如下：
//
//死代码删除优化：
//
//
//基本块内部的死代码清理：
//
//找到基本块中的终止指令（跳转、分支或返回）
//删除终止指令之后的所有指令，因为它们永远不会被执行
//
//
//不可达基本块的删除：
//
//从函数入口块开始进行可达性分析
//递归标记所有可达的基本块
//删除未被标记的基本块，这些块是不可达的
//清理被删除块的所有指令和操作数引用
//
//
//
//
//基本块合并优化：
//合并满足以下条件的基本块：
//
//
//源块只有一个后继块
//目标块只有一个前驱块（即源块）
//合并过程包括：
//删除源块的跳转指令
//将目标块的指令移到源块
//特殊处理目标块中的Phi指令
//更新所有的引用关系
//标记目标块为已删除
//
//这些优化可以：
//
//消除永远不会执行的代码
//减少跳转指令
//提高程序的局部性
//简化控制流图结构
//为后续优化创造更好的条件
//
//通过这些优化可以提高代码质量和执行效率。整个优化过程保持了程序的语义不变性，同时减少了不必要的指令和控制流转移。