package midEnd.helper;



import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;
import LLVMIR.IRBuilder;
import LLVMIR.Base.Instruction;


import LLVMIR.Ins.Branch;
import LLVMIR.Ins.Phi;
import LLVMIR.Ins.Ret;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class delete {
    private static HashSet<BasicBlock> reachableBlocks;

    public static void rearrange(Module module) {
        for (Function function : module.getFunctions()) {
            BasicBlock firstBlock = function.getBasicBlocks().get(0);
            ArrayList<BasicBlock> blocks = new ArrayList<>(function.getBasicBlocks());

            for (BasicBlock block : blocks) {
                Instruction lastInstruction = block.getInstrs().get(block.getInstrs().size() - 1);

                if (lastInstruction instanceof Ret) continue;

                BasicBlock targetBlock;
                if (!((Branch)lastInstruction).isConditional()) {
                    targetBlock = ((Branch) lastInstruction).getTargetBlock();
                } else {
                    targetBlock = ((Branch) lastInstruction).getElseBlock();
                }

                if (targetBlock != block) {
                    function.getBasicBlocks().remove(block);
                    function.getBasicBlocks().add(function.getBasicBlocks().indexOf(targetBlock), block);
                }
            }

            if (function.getBasicBlocks().get(0) != firstBlock) {
                BasicBlock newEntryBlock = new BasicBlock(IRBuilder.blockName + function.getBlockId(), function);
                Branch jumpToFirstBlock = new Branch(firstBlock, newEntryBlock);
                newEntryBlock.addInstr(jumpToFirstBlock);
                function.getBasicBlocks().add(0, newEntryBlock);
            }

            if (function.getName().equals("@main")) {
                List<Instruction> lastBlockInstructions = function.getBasicBlocks()
                        .get(function.getBasicBlocks().size() - 1)
                        .getInstrs();
                if (lastBlockInstructions.get(lastBlockInstructions.size() - 1) instanceof Ret &&
                        module.getFunctions().size() == 1) {
                    lastBlockInstructions.remove(lastBlockInstructions.size() - 1);
                }
            }
        }
    }

    public static void simplify(Module module) {
        for (Function function : module.getFunctions()) {
            // 先清理死代码
            for (BasicBlock block : function.getBasicBlocks()) {
                removeDeadInstructions(block);
            }

            // 标记可达块
            BasicBlock entryBlock = function.getBasicBlocks().get(0);
            reachableBlocks = new HashSet<>();
            findReachableBlocks(entryBlock);

            // 删除不可达块
            Iterator<BasicBlock> iterator = function.getBasicBlocks().iterator();
            while (iterator.hasNext()) {
                BasicBlock block = iterator.next();
                if (!reachableBlocks.contains(block)) {
                    // 清理块内指令的操作数
                    for (Instruction instr : block.getInstrs()) {
                        instr.removeOperands();
                    }
                    // 清理块本身的操作数
                    block.removeOperands();
                    // 从函数中移除
                    iterator.remove();
                    // 标记为已删除
                    block.setDeleted();
                }
            }
        }
    }

    public static void removeDeadInstructions(BasicBlock block) {
        List<Instruction> instructions = block.getInstrs();
        int index = 0;

        while (true) {
            Instruction.InstrType type = instructions.get(index).getInstrType();
            if (type == Instruction.InstrType.JUMP ||
                    type == Instruction.InstrType.BRANCH ||
                    type == Instruction.InstrType.RETURN) {
                break;
            }
            index++;
        }

        index++;
        while (index < instructions.size()) {
            instructions.get(index).removeOperands();
            instructions.remove(index);
        }
    }

    public static void findReachableBlocks(BasicBlock block) {
        if (reachableBlocks.contains(block)) {
            return;
        }

        reachableBlocks.add(block);
        Instruction lastInstruction = block.getInstrs().get(block.getInstrs().size() - 1);

        if (lastInstruction instanceof Branch && !((Branch) lastInstruction).isConditional()) {
            findReachableBlocks(((Branch) lastInstruction).getTargetBlock());
        } else if (lastInstruction instanceof Branch && ((Branch) lastInstruction).isConditional()) {
            findReachableBlocks(((Branch) lastInstruction).getThenBlock());
            findReachableBlocks(((Branch) lastInstruction).getElseBlock());
        }
    }

    public static void mergeBlocks(Module module) {
        for (Function function : module.getFunctions()) {
            for (BasicBlock block : function.getBasicBlocks()) {
                if (!block.isDeleted() && block.getSuccessors().size() == 1) {
                    BasicBlock successor = block.getSuccessors().get(0);

                    if (successor.getPredecessors().size() == 1) {
                        Instruction jumpInstruction = block.getInstrs().remove(block.getInstrs().size() - 1);

                        for (Instruction instruction : successor.getInstrs()) {
                            if (instruction instanceof Phi) {
                                Phi phi = (Phi) instruction;
                                phi.modifyValueForUsers(phi.getOperands().get(phi.getIncomingBlocks().indexOf(block)));
                                phi.removeOperands();
                            } else {
                                block.addInstr(instruction);
                                instruction.setParentBlock(block);
                            }
                        }

                        successor.modifyValueForUsers(block);
                        successor.setDeleted();
                    }
                }
            }

            function.getBasicBlocks().removeIf(BasicBlock::isDeleted);
        }
    }
}
