package midEnd.mem;

import LLVMIR.Ins.Alloca;
import LLVMIR.Base.*;
import LLVMIR.Global.Function;
import LLVMIR.IRBuilder;
import LLVMIR.Ins.Load;
import LLVMIR.Ins.Phi;
import LLVMIR.Ins.Store;
import LLVMIR.Base.Module;
import LLVMIR.LLVMType.LLVMType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/**
 * 内存访问优化器，将内存操作转换为寄存器操作
 * 实现了mem2reg优化，通过插入phi节点和重命名变量实现
 */
public class MemToReg {
    // 当前处理的内存分配指令
    private static Alloca activeAllocaInstr;
    // 定义和使用指令列表
    private static ArrayList<Instruction> definitionInstrs;
    private static ArrayList<Instruction> usageInstrs;
    // 定义和使用所在的基本块
    private static ArrayList<BasicBlock> definitionBlocks;
    private static ArrayList<BasicBlock> usageBlocks;
    // 定义值栈
    private static Stack<Value> definitionStack;

    /**
     * 执行内存到寄存器的优化转换
     * @param module LLVM IR模块
     */
    public static void execute(Module module) {
        for (Function function : module.getFunctions()) {
            for (BasicBlock block : function.getBasicBlocks()) {
                ArrayList<Instruction> instructions = new ArrayList<>(block.getInstrs());
                for (Instruction instruction : instructions) {
                    if (isEligibleAlloca(instruction)) {
                        activeAllocaInstr = (Alloca) instruction;
                        initializeDataStructures();
                        insertPhiNodes();
                        performVariableRenaming(function.getBasicBlocks().get(0));
                    }
                }
            }
        }
    }

    /**
     * 检查是否为可优化的内存分配指令
     */
    private static boolean isEligibleAlloca(Instruction instruction) {
        return instruction instanceof Alloca &&
                (((Alloca) instruction).getPointedType() == LLVMType.Int32 ||
                        ((Alloca) instruction).getPointedType() == LLVMType.Int8);
    }

    /**
     * 初始化数据结构
     */
    private static void initializeDataStructures() {
        usageBlocks = new ArrayList<>();
        usageInstrs = new ArrayList<>();
        definitionBlocks = new ArrayList<>();
        definitionInstrs = new ArrayList<>();
        definitionStack = new Stack<>();

        // 收集所有使用和定义点
        for (User user : activeAllocaInstr.getUsers()) {
            if (!(user instanceof Instruction)) continue;

            Instruction instruction = (Instruction) user;
            if (instruction.getParentBlock().isDeleted()) continue;

            if (instruction instanceof Load) {
                processLoadInstruction(instruction);
            } else if (instruction instanceof Store) {
                processStoreInstruction(instruction);
            }
        }
    }

    /**
     * 处理加载指令
     */
    private static void processLoadInstruction(Instruction instruction) {
        usageInstrs.add(instruction);
        BasicBlock parentBlock = instruction.getParentBlock();
        if (!usageBlocks.contains(parentBlock)) {
            usageBlocks.add(parentBlock);
        }
    }

    /**
     * 处理存储指令
     */
    private static void processStoreInstruction(Instruction instruction) {
        definitionInstrs.add(instruction);
        BasicBlock parentBlock = instruction.getParentBlock();
        if (!definitionBlocks.contains(parentBlock)) {
            definitionBlocks.add(parentBlock);
        }
    }

    /**
     * 插入phi节点
     */
    private static void insertPhiNodes() {
        HashSet<BasicBlock> processedBlocks = new HashSet<>();
        ArrayList<BasicBlock> workList = new ArrayList<>(definitionBlocks);

        while (!workList.isEmpty()) {
            BasicBlock currentBlock = workList.remove(0);
            for (BasicBlock dominanceBlock : currentBlock.getDF()) {
                if (!processedBlocks.contains(dominanceBlock)) {
                    insertPhiAtBlockStart(dominanceBlock);
                    processedBlocks.add(dominanceBlock);
                    if (!definitionBlocks.contains(dominanceBlock)) {
                        workList.add(dominanceBlock);
                    }
                }
            }
        }
    }

    /**
     * 在基本块开始处插入phi节点
     */
    private static void insertPhiAtBlockStart(BasicBlock block) {
        String varName = IRBuilder.tempName + block.getParentFunc().getVarId();
        ArrayList<BasicBlock> predecessors = new ArrayList<>(block.getPredecessors());

        Phi phiInstruction = new Phi(varName, block, predecessors, activeAllocaInstr.getPointedType());
        block.getInstrs().add(0, phiInstruction);
        usageInstrs.add(phiInstruction);
        definitionInstrs.add(phiInstruction);
    }

    /**
     * 执行变量重命名过程
     */
    private static void performVariableRenaming(BasicBlock block) {
        Iterator<Instruction> iterator = block.getInstrs().iterator();
        int pushCount = 0;

        // 处理当前基本块中的指令
        while (iterator.hasNext()) {
            Instruction instruction = iterator.next();

            if (instruction == activeAllocaInstr) {
                // 移除原始的alloca指令
                instruction.removeOperands();
                iterator.remove();
            } else if (isLoadInstruction(instruction)) {
                processLoadRenaming(instruction, iterator);
            } else if (isStoreInstruction(instruction)) {
                pushCount += processStoreRenaming(instruction, iterator);
            } else if (isPhiInstruction(instruction)) {
                pushCount += processPhiRenaming(instruction);
            }
        }

        // 处理后继基本块中的phi节点
        processSuccessorPhiNodes(block);

        // 递归处理支配树中的子节点
        for (BasicBlock dominated : block.getImdom()) {
            performVariableRenaming(dominated);
        }

        // 恢复栈状态
        for (int i = 0; i < pushCount; i++) {
            definitionStack.pop();
        }
    }

    /**
     * 检查是否为需要处理的加载指令
     */
    private static boolean isLoadInstruction(Instruction instruction) {
        return instruction instanceof Load && usageInstrs.contains(instruction);
    }

    /**
     * 检查是否为需要处理的存储指令
     */
    private static boolean isStoreInstruction(Instruction instruction) {
        return instruction instanceof Store && definitionInstrs.contains(instruction);
    }

    /**
     * 检查是否为需要处理的phi指令
     */
    private static boolean isPhiInstruction(Instruction instruction) {
        return instruction instanceof Phi && definitionInstrs.contains(instruction);
    }

    /**
     * 处理加载指令的重命名
     */
    private static void processLoadRenaming(Instruction instruction, Iterator<Instruction> iterator) {
        Value newValue = definitionStack.empty() ? new Undef() : definitionStack.peek();
        instruction.modifyValueForUsers(newValue);
        instruction.removeOperands();
        iterator.remove();
    }

    /**
     * 处理存储指令的重命名
     * @return 压栈次数
     */
    private static int processStoreRenaming(Instruction instruction, Iterator<Instruction> iterator) {
        Value storedValue = ((Store) instruction).getFrom();
        definitionStack.push(storedValue);
        instruction.removeOperands();
        iterator.remove();
        return 1;
    }

    /**
     * 处理phi指令的重命名
     * @return 压栈次数
     */
    private static int processPhiRenaming(Instruction instruction) {
        definitionStack.push(instruction);
        return 1;
    }

    /**
     * 处理后继基本块中的phi节点
     */
    private static void processSuccessorPhiNodes(BasicBlock block) {
        for (BasicBlock successor : block.getSuccessors()) {
            if (!successor.getInstrs().isEmpty()) {
                Instruction firstInstruction = successor.getInstrs().get(0);
                if (firstInstruction instanceof Phi && usageInstrs.contains(firstInstruction)) {
                    Value value = definitionStack.empty() ? new Undef() : definitionStack.peek();
                    if (value == null) {
                        value = new Undef();
                    }
                    ((Phi) firstInstruction).addIncomingValue(block, value);
                }
            }
        }
    }
}