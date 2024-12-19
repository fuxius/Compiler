package midEnd.mem;

import LLVMIR.Base.*;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;
import LLVMIR.IRBuilder;
import LLVMIR.Ins.Branch;
import LLVMIR.Ins.Move;
import LLVMIR.Ins.Phi;
import LLVMIR.LLVMType.LLVMType;
import backEnd.Base.Register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Phi指令消除器
 * 负责将LLVM IR中的Phi指令转换为Move指令，并处理相关的并行性和寄存器分配问题
 */
public class Remove {
    // 变量到寄存器的映射关系
    private static HashMap<Value, Register> variableToRegisterMap;

    /**
     * 移除模块中的所有Phi指令
     * @param module 待处理的LLVM IR模块
     */
    public static void removePhi(Module module) {
        for (Function function : module.getFunctions()) {
            variableToRegisterMap = function.getRegisterPool();
            ArrayList<BasicBlock> blocks = new ArrayList<>(function.getBasicBlocks());
            for (BasicBlock block : blocks) {
                processPhiInstructions(block);
            }
        }
    }

    /**
     * 处理基本块中的Phi指令
     * @param block 当前处理的基本块
     */
    private static void processPhiInstructions(BasicBlock block) {
        // 获取当前基本块中的所有Phi指令及其相关Move指令
        HashMap<BasicBlock, ArrayList<Move>> predecessorMoves = collectPhiInstructions(block);

        // 处理每个前驱基本块
        ArrayList<BasicBlock> predecessors = new ArrayList<>(block.getPredecessors());
        for (BasicBlock predecessor : predecessors) {
            ArrayList<Move> moves = predecessorMoves.get(predecessor);
            if (moves.isEmpty()) {
                continue;
            }

            // 处理Move指令的并行性
            ArrayList<Move> parallelMoves = handleMoveParallelism(moves, block);

            // 处理寄存器共享问题
            ArrayList<Move> finalMoves = handleRegisterSharing(parallelMoves, block);

            // 插入Move指令
            insertMoveInstructions(predecessor, finalMoves, block);
        }
    }

    /**
     * 收集基本块中的Phi指令并转换为Move指令
     */
    private static HashMap<BasicBlock, ArrayList<Move>> collectPhiInstructions(BasicBlock block) {
        HashMap<BasicBlock, ArrayList<Move>> predecessorMoves = new HashMap<>();
        for (BasicBlock predecessor : block.getPredecessors()) {
            predecessorMoves.put(predecessor, new ArrayList<>());
        }

        Iterator<Instruction> iterator = block.getInstrs().iterator();
        while (iterator.hasNext()) {
            Instruction instruction = iterator.next();
            if (!(instruction instanceof Phi)) {
                break;
            }

            Phi phiInstruction = (Phi) instruction;
            processPhiInstruction(phiInstruction, predecessorMoves, block);
            iterator.remove();
        }

        return predecessorMoves;
    }

    /**
     * 处理单个Phi指令
     */
    private static void processPhiInstruction(Phi phi,
                                              HashMap<BasicBlock, ArrayList<Move>> predecessorMoves,
                                              BasicBlock block) {
        List<Value> operands = phi.getOperands();
        ArrayList<BasicBlock> incomingBlocks = phi.getIncomingBlocks();

        // 清理无效的前驱块
        for (int i = incomingBlocks.size() - 1; i >= 0; i--) {
            if (!block.getPredecessors().contains(incomingBlocks.get(i))) {
                incomingBlocks.remove(i);
                operands.remove(i);
            }
        }

        // 为每个有效操作数创建Move指令
        for (int i = 0; i < operands.size(); i++) {
            Value operand = operands.get(i);
            if (operand instanceof Undef) {
                continue;
            }

            BasicBlock incomingBlock = incomingBlocks.get(i);
            createMoveInstruction(phi, operand, incomingBlock, predecessorMoves);
        }
    }

    /**
     * 创建Move指令
     */
    private static void createMoveInstruction(Phi phi,
                                              Value source,
                                              BasicBlock incomingBlock,
                                              HashMap<BasicBlock, ArrayList<Move>> predecessorMoves) {
        Move move = new Move(phi, source, incomingBlock);
        if (phi.getType() == LLVMType.Int8) {
            move.setType(LLVMType.Int8);
        }
        predecessorMoves.get(incomingBlock).add(move);
    }

    /**
     * 处理Move指令的并行性
     */
    private static ArrayList<Move> handleMoveParallelism(ArrayList<Move> originalMoves, BasicBlock block) {
        ArrayList<Move> parallelMoves = new ArrayList<>();

        for (int i = 0; i < originalMoves.size(); i++) {
            for (int j = i + 1; j < originalMoves.size(); j++) {
                if (originalMoves.get(i).getTo() == originalMoves.get(j).getFrom()) {
                    // 创建临时变量解决依赖
                    Value tempVar = createTemporaryVariable(originalMoves.get(i).getTo(), block);
                    Move tempMove = new Move(tempVar, originalMoves.get(i).getTo(), block);
                    parallelMoves.add(0, tempMove);

                    // 更新后续使用
                    for (int k = j; k < originalMoves.size(); k++) {
                        if (originalMoves.get(i).getTo() == originalMoves.get(k).getFrom()) {
                            originalMoves.get(k).setFrom(tempVar);
                        }
                    }
                }
            }
            parallelMoves.add(originalMoves.get(i));
        }

        return parallelMoves;
    }

    /**
     * 处理寄存器共享问题
     */
    private static ArrayList<Move> handleRegisterSharing(ArrayList<Move> parallelMoves, BasicBlock block) {
        ArrayList<Move> finalMoves = new ArrayList<>();

        for (int i = 0; i < parallelMoves.size(); i++) {
            for (int j = i + 1; j < parallelMoves.size(); j++) {
                if (sharesRegister(parallelMoves.get(i), parallelMoves.get(j))) {
                    // 创建临时变量解决寄存器冲突
                    Value tempVar = createTemporaryVariable(parallelMoves.get(i).getTo(), block);
                    Move tempMove = new Move(tempVar, parallelMoves.get(i).getTo(), block);
                    finalMoves.add(0, tempMove);

                    // 更新后续使用
                    for (int k = j; k < parallelMoves.size(); k++) {
                        if (sharesRegister(parallelMoves.get(i), parallelMoves.get(k))) {
                            parallelMoves.get(k).setFrom(tempVar);
                        }
                    }
                }
            }
            finalMoves.add(parallelMoves.get(i));
        }

        return finalMoves;
    }

    /**
     * 检查两个Move指令是否共享寄存器
     */
    private static boolean sharesRegister(Move move1, Move move2) {
        return variableToRegisterMap.containsKey(move1.getTo()) &&
                variableToRegisterMap.containsKey(move2.getFrom()) &&
                variableToRegisterMap.get(move1.getTo()) == variableToRegisterMap.get(move2.getFrom());
    }

    /**
     * 创建临时变量
     */
    private static Value createTemporaryVariable(Value original, BasicBlock block) {
        return new Value(IRBuilder.tempName + block.getParentFunc().getVarId(),
                original.getType());
    }

    /**
     * 插入Move指令到适当的位置
     */
    private static void insertMoveInstructions(BasicBlock predecessor,
                                               ArrayList<Move> moves,
                                               BasicBlock targetBlock) {
        if (predecessor.getSuccessors().size() > 1) {
            // 创建新的基本块处理多个后继的情况
            insertMovesInNewBlock(predecessor, moves, targetBlock);
        } else {
            // 直接在前驱块中插入Move指令
            insertMovesInPredecessor(predecessor, moves);
        }
    }

    /**
     * 在新的基本块中插入Move指令
     */
    private static void insertMovesInNewBlock(BasicBlock predecessor,
                                              ArrayList<Move> moves,
                                              BasicBlock targetBlock) {
        BasicBlock newBlock = createIntermediateBlock(predecessor, targetBlock);

        // 添加Move指令
        for (Move move : moves) {
            newBlock.addInstr(move);
        }

        // 添加跳转指令
        Branch jumpInstruction = new Branch(targetBlock, newBlock);
        newBlock.addInstr(jumpInstruction);

        // 更新原始跳转指令
        updateBranchTarget(predecessor, targetBlock, newBlock);
    }

    /**
     * 创建中间基本块
     */
    private static BasicBlock createIntermediateBlock(BasicBlock predecessor, BasicBlock targetBlock) {
        String blockName = IRBuilder.blockName + targetBlock.getParentFunc().getBlockId();
        BasicBlock newBlock = new BasicBlock(blockName, targetBlock.getParentFunc());

        List<BasicBlock> blocks = targetBlock.getParentFunc().getBasicBlocks();
        blocks.add(blocks.indexOf(targetBlock), newBlock);

        return newBlock;
    }

    /**
     * 更新分支指令的目标
     */
    private static void updateBranchTarget(BasicBlock predecessor,
                                           BasicBlock oldTarget,
                                           BasicBlock newTarget) {
        Branch branch = (Branch) predecessor.getInstrs().get(predecessor.getInstrs().size() - 1);
        branch.getOperands().set(branch.getOperands().indexOf(oldTarget), newTarget);
    }

    /**
     * 在前驱块中直接插入Move指令
     */
    private static void insertMovesInPredecessor(BasicBlock predecessor, ArrayList<Move> moves) {
        for (Move move : moves) {
            predecessor.getInstrs().add(predecessor.getInstrs().size() - 1, move);
        }
    }
}