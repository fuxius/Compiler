package midEnd.Var;

import LLVMIR.Base.*;
import LLVMIR.Base.Core.User;
import LLVMIR.Base.Core.Value;
import LLVMIR.Base.Core.Module;
import LLVMIR.Ins.*;
import LLVMIR.Global.Function;
import LLVMIR.Ins.Mem.Load;
import LLVMIR.Ins.Mem.Phi;
import LLVMIR.LLVMType.PointerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class GCM {
    // 当前处理的函数
    private static Function currentFunction;
    // 存储已访问的指令，防止重复处理
    private static HashSet<Instruction> visitedInstructions = new HashSet<>();

    /**
     * 移动模块中的所有函数的指令
     * @param module LLVM IR 模块
     */
    public static void moveInstrs(Module module) {
        for (Function function : module.getFunctions()) {
            currentFunction = function;
            processFunctionInstructions(function);
        }
    }

    /**
     * 处理单个函数中的指令移动
     * @param function 当前处理的函数
     */
    public static void processFunctionInstructions(Function function) {
        visitedInstructions.clear();
        currentFunction = function;
        // 获取支配树的后序遍历并反转
        ArrayList<BasicBlock> postOrderBlocks = function.getPostOrderForIdomTree();
        Collections.reverse(postOrderBlocks);
        // 收集所有指令
        ArrayList<Instruction> instructionList = collectAllInstructions(postOrderBlocks);
        // 早期调度
        for (Instruction instr : instructionList) {
            scheduleEarly(instr);
        }
        visitedInstructions.clear();
        // 反转指令列表进行后期调度
        Collections.reverse(instructionList);
        for (Instruction instr : instructionList) {
            scheduleLate(instr);
        }
    }

    /**
     * 收集所有基本块中的指令
     * @param blocks 基本块列表
     * @return 所有指令的列表
     */
    private static ArrayList<Instruction> collectAllInstructions(ArrayList<BasicBlock> blocks) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        for (BasicBlock block : blocks) {
            instructions.addAll(block.getInstrs());
        }
        return instructions;
    }

    /**
     * 提前调度指令，将指令尽可能移动到最早的位置
     * @param instr 需要移动的指令
     */
    public static void scheduleEarly(Instruction instr) {
        // 如果指令不可移动或已访问，则跳过
        if (!isMovable(instr) || visitedInstructions.contains(instr)) {
            return;
        }
        visitedInstructions.add(instr);
        // 获取当前函数的第一个基本块
        BasicBlock firstBlock = currentFunction.getBasicBlocks().get(0);
        // 从原块中移除指令
        instr.getParentBlock().getInstrs().remove(instr);
        // 将指令添加到第一个块的末尾前一个位置
        firstBlock.getInstrs().add(firstBlock.getInstrs().size() - 1, instr);
        // 更新指令的父块
        instr.setParentBlock(firstBlock);
        // 递归提前调度操作数中的指令
        for (Value operand : instr.getOperands()) {
            if (operand instanceof Instruction) {
                Instruction operandInstr = (Instruction) operand;
                scheduleEarly(operandInstr);
                // 如果操作数指令所在块的深度大于当前指令所在块的深度，则调整当前指令的位置
                if (instr.getParentBlock().getImdomDepth() < operandInstr.getParentBlock().getImdomDepth()) {
                    moveInstructionToBlock(instr, operandInstr.getParentBlock());
                }
            }
        }
    }

    /**
     * 将指令移动到指定的基本块
     * @param instr 需要移动的指令
     * @param targetBlock 目标基本块
     */
    private static void moveInstructionToBlock(Instruction instr, BasicBlock targetBlock) {
        instr.getParentBlock().getInstrs().remove(instr);
        targetBlock.getInstrs().add(targetBlock.getInstrs().size() - 1, instr);
        instr.setParentBlock(targetBlock);
    }

    /**
     * 查找两个基本块在支配树中的最近公共祖先
     * @param block1 第一个基本块
     * @param block2 第二个基本块
     * @return 最近公共祖先的基本块
     */
    public static BasicBlock findLowestCommonAncestor(BasicBlock block1, BasicBlock block2) {
        if (block1 == null) {
            return block2;
        }
        // 将块1和块2提升到相同的支配深度
        while (block1.getImdomDepth() < block2.getImdomDepth()) {
            block2 = block2.getImmediateDominator();
        }
        while (block2.getImdomDepth() < block1.getImdomDepth()) {
            block1 = block1.getImmediateDominator();
        }
        // 同时提升直到找到共同的支配块
        while (block1 != block2) {
            block1 = block1.getImmediateDominator();
            block2 = block2.getImmediateDominator();
        }
        return block1;
    }

    /**
     * 延后调度指令，将指令尽可能移动到最晚的位置
     * @param instr 需要移动的指令
     */
    public static void scheduleLate(Instruction instr) {
        // 如果指令不可移动或已访问，则跳过
        if (!isMovable(instr) || visitedInstructions.contains(instr)) {
            return;
        }
        visitedInstructions.add(instr);
        // 查找指令的用户的最近公共祖先
        BasicBlock lcaBlock = findUsersLowestCommonAncestor(instr);
        if (lcaBlock == null) {
            return;
        }
        // 将指令移动到最近公共祖先所在的块
        BasicBlock targetBlock = determineTargetBlock(instr, lcaBlock);
        if (targetBlock != null) {
            moveInstructionToBlock(instr, targetBlock);
            // 确保指令在使用它的指令之前
            reorderInstruction(instr, targetBlock);
        }
    }

    /**
     * 查找指令的所有用户的最近公共祖先
     * @param instr 需要查找的指令
     * @return 最近公共祖先的基本块
     */
    private static BasicBlock findUsersLowestCommonAncestor(Instruction instr) {
        BasicBlock commonAncestor = null;
        for (User user : instr.getUsers()) {
            if (user instanceof Instruction) {
                Instruction userInstr = (Instruction) user;
                scheduleLate(userInstr);
                BasicBlock userBlock = getUserBlock(user, instr);
                commonAncestor = findLowestCommonAncestor(commonAncestor, userBlock);
            }
        }
        return commonAncestor;
    }

    /**
     * 获取用户指令的基本块
     * @param user 用户
     * @param instr 当前指令
     * @return 用户指令所在的基本块
     */
    private static BasicBlock getUserBlock(User user, Instruction instr) {
        if (user instanceof Phi) {
            Phi phiUser = (Phi) user;
            for (int i = 0; i < phiUser.getOperands().size(); i++) {
                if (phiUser.getOperands().get(i) == instr) {
                    return phiUser.getIncomingBlocks().get(i);
                }
            }
        }
        return ((Instruction) user).getParentBlock();
    }

    /**
     * 确定目标基本块，将指令移动到该块
     * @param instr 当前指令
     * @param lcaBlock 最近公共祖先块
     * @return 目标基本块
     */
    private static BasicBlock determineTargetBlock(Instruction instr, BasicBlock lcaBlock) {
        BasicBlock target = lcaBlock;
        while (lcaBlock != instr.getParentBlock()) {
            if (lcaBlock == null) {
                return null;
            }
            lcaBlock = lcaBlock.getImmediateDominator();
            if (lcaBlock.getLoopDepth() < target.getLoopDepth()) {
                target = lcaBlock;
            }
        }
        return target;
    }

    /**
     * 确保指令在使用它的指令之前
     * @param instr 指令
     * @param block 指令所在的块
     */
    private static void reorderInstruction(Instruction instr, BasicBlock block) {
        for (Instruction blockInstr : new ArrayList<>(block.getInstrs())) {
            if (blockInstr != instr && !(blockInstr instanceof Phi) && blockInstr.getOperands().contains(instr)) {
                block.getInstrs().remove(instr);
                int index = block.getInstrs().indexOf(blockInstr);
                block.getInstrs().add(index, instr);
                break;
            }
        }
    }

    /**
     * 判断指令是否可以被移动
     * @param instr 需要判断的指令
     * @return 如果指令可移动则返回true，否则返回false
     */
    public static boolean isMovable(Instruction instr) {
        // 检查指令类型是否为可移动的类型
        if (instr instanceof Alu ||
                instr instanceof GetPtr ||
                instr instanceof Icmp ||
                instr instanceof Zext) {
            return true;
        }
        // 如果是调用指令，进一步检查
        if (instr instanceof Call) {
            Call callInstr = (Call) instr;
            Function calleeFunction = (Function) callInstr.getOperands().get(0);
            // 如果被调用函数有副作用或调用自身，则不可移动
            if (calleeFunction.isHasSideEffects() || instr.getParentBlock().getParentFunc() == calleeFunction) {
                return false;
            }
            // 如果调用指令没有用户，则不可移动
            if (callInstr.getUsers().isEmpty()) {
                return false;
            }
            // 检查调用指令的所有用户
            for (Value user : callInstr.getUsers()) {
                if (user instanceof GetPtr || user instanceof Load || user.getType() instanceof PointerType) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
//这个全局代码移动(Global Code Motion)优化的主要思路是：
//将指令移动到更优的位置，以减少执行次数，同时保持程序的正确性。整个过程分为两个主要阶段：
//
//早期调度(Schedule Early)：
//
//
//将指令尽可能往前移动到最早可以执行的位置
//主要步骤：
//
//从函数的第一个基本块开始
//递归处理指令的所有操作数
//确保指令位置在其所有操作数之后
//根据支配树深度调整指令位置
//
//
//
//
//延后调度(Schedule Late)：
//
//
//将指令尽可能往后移动到最晚需要的位置
//主要步骤：
//
//找到所有使用该指令的点
//计算这些使用点的最近公共祖先(LCA)
//在保证正确性的前提下，选择循环嵌套最浅的位置
//确保指令在使用它的指令之前
//
//
//
//可移动的指令类型包括：
//
//算术运算(Alu)
//指针计算(GetPtr)
//比较指令(Icmp)
//类型扩展(Zext)
//某些无副作用的函数调用(Call)
//
//优化的主要目标：
//
//减少循环中的计算次数
//提高指令级并行性
//改善寄存器分配
//减少执行路径长度
//
//这个优化在保证程序正确性的同时，通过调整指令位置来提高性能，特别是对于循环中的不变计算，可以将其移出循环，显著减少执行次数。