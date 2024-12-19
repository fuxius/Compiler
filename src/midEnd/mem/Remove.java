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
 * Remove类用于移除LLVM IR中的Phi指令，并将其转换为Move指令。
 */
public class Remove {
    private static HashMap<Value, Register> var2reg; // 变量到寄存器的映射

    /**
     * 移除模块中的所有Phi指令。
     * @param module 要处理的模块
     */
    public static void removePhi(Module module) {
        for (Function func : module.getFunctions()) { // 遍历模块中的每个函数
            var2reg = func.getRegisterPool(); // 获取函数的寄存器池
            ArrayList<BasicBlock> blocks = new ArrayList<>(func.getBasicBlocks()); // 获取函数的基本块列表
            for (BasicBlock block : blocks) {
                removeBlockPhi(block); // 移除基本块中的Phi指令
            }
        }
    }

    /**
     * 移除基本块中的Phi指令。
     * @param curBlock 当前处理的基本块
     */
    public static void removeBlockPhi(BasicBlock curBlock) {
        System.out.println("\n=== Start processing block: " + curBlock.getName() + " ===");

        // 打印所有指令
        System.out.println("All instructions in block:");
        for (Instruction instr : curBlock.getInstrs()) {
            System.out.println("- " + instr.getClass().getSimpleName() + ": " + instr);
        }
        // 检查是否有 Phi 指令
        boolean hasPhiInstr = false;
        for (Instruction instr : curBlock.getInstrs()) {
            if (instr instanceof Phi) {
                hasPhiInstr = true;
                break;
            }
        }
        System.out.println("Has Phi instructions: " + hasPhiInstr);
        Iterator<Instruction> it = curBlock.getInstrs().iterator(); // 获取当前基本块的指令迭代器
        HashMap<BasicBlock, ArrayList<Move>> parent2moves = new HashMap<>(); // 父基本块到Move指令的映射
        for (BasicBlock parent : curBlock.getPredecessors()) { // 遍历当前基本块的前驱基本块
            parent2moves.put(parent, new ArrayList<>()); // 初始化映射
        }
        while (it.hasNext()) { // 遍历当前基本块的指令
            Instruction instr = it.next(); // 获取下一条指令
            if (!(instr instanceof Phi)) { // 如果不是Phi指令
                break; // 跳出循环
            }
            Phi phiInstr = (Phi) instr; // 将指令转换为Phi指令
            List<Value> operands = phiInstr.getOperands(); // 获取Phi指令的操作数
            ArrayList<BasicBlock> parents = phiInstr.getIncomingBlocks(); // 获取Phi指令的前驱基本块
            for (int i = 0; i <= parents.size() - 1; i++) { // 遍历前驱基本块
                if (!curBlock.getPredecessors().contains(parents.get(i))) { // 如果当前基本块的前驱不包含该前驱基本块
                    parents.remove(i); // 移除该前驱基本块
                    operands.remove(i); // 移除对应的操作数
                    i--; // 调整索引
                }
            }
            for (int i = 0; i <= operands.size() - 1; i++) { // 遍历操作数
                if (!(operands.get(i) instanceof Undef)) { // 如果操作数不是Undef
                    Value from = operands.get(i);
                    if (phiInstr.getType() == LLVMType.Int8) {
                        // 如果是字符类型，需要特别处理
                            Move move = new Move(phiInstr, from, parents.get(i));
                            // 确保在创建 Move 指令时保留类型信息
                            move.setType(LLVMType.Int8);
                            parent2moves.get(parents.get(i)).add(move);
                    }
                    Move move = new Move(phiInstr, from, parents.get(i));
                    parent2moves.get(parents.get(i)).add(move);
                }
            }
            it.remove(); // 移除Phi指令
        }
        ArrayList<BasicBlock> parents = new ArrayList<>(curBlock.getPredecessors()); // 获取当前基本块的前驱基本块列表
        for (BasicBlock parent : parents) { // 遍历前驱基本块
            if (parent2moves.get(parent).size() == 0) continue; // 如果父基本块的Move指令列表为空，跳过
            // 保证指令的并行性
            ArrayList<Move> paralleledMoves = new ArrayList<>(); // 并行的Move指令列表
            ArrayList<Move> oriMoves = parent2moves.get(parent); // 原始的Move指令列表

            for (int i = 0; i <= oriMoves.size() - 1; i++) { // 遍历原始的Move指令列表
                for (int j = i + 1; j <= oriMoves.size() - 1; j++) { // 遍历原始的Move指令列表
                    if (oriMoves.get(i).getTo() == oriMoves.get(j).getFrom()) { // 如果目标寄存器和源寄存器相同
                        Value value = new Value(IRBuilder.tempName + curBlock.getParentFunc().getVarId(), oriMoves.get(i).getTo().getType()); // 创建临时变量
                        Move temp = new Move(value, oriMoves.get(i).getTo(), curBlock); // 创建临时Move指令
                        System.out.println("Phi instruction type: " + value.getType());
                        System.out.println("Operand value: " + oriMoves.get(i).getTo());
                        System.out.println("Operand type: " + oriMoves.get(i).getTo().getType());
                        paralleledMoves.add(0, temp); // 将临时Move指令添加到并行的Move指令列表中
                        for (int k = j; k <= oriMoves.size() - 1; k++) { // 遍历原始的Move指令列表
                            if (oriMoves.get(i).getTo() == oriMoves.get(k).getFrom()) { // 如果目标寄存器和源寄存器相同
                                oriMoves.get(k).setFrom(value); // 设置源寄存器为临时变量
                            }
                        }
                    }
                }
                paralleledMoves.add(oriMoves.get(i)); // 将原始的Move指令添加到并行的Move指令列表中
            }
            // 解决指令共享寄存器的问题
            ArrayList<Move> finalMoves = new ArrayList<>(); // 最终的Move指令列表
            for (int i = 0; i <= paralleledMoves.size() - 1; i++) { // 遍历并行的Move指令列表
                for (int j = i + 1; j <= paralleledMoves.size() - 1; j++) { // 遍历并行的Move指令列表
                    if (var2reg.containsKey(paralleledMoves.get(i).getTo()) && // 如果目标寄存器在寄存器映射中
                            var2reg.containsKey(paralleledMoves.get(j).getFrom()) && // 如果源寄存器在寄存器映射中
                            var2reg.get(paralleledMoves.get(i).getTo()) == var2reg.get(paralleledMoves.get(j).getFrom())) { // 如果目标寄存器和源寄存器相同
                        Value value = new Value(IRBuilder.tempName + curBlock.getParentFunc().getVarId(), paralleledMoves.get(i).getTo().getType()); // 创建临时变量
                        Move temp = new Move(value, paralleledMoves.get(i).getTo(), curBlock); // 创建临时Move指令
                        System.out.println("Phi instruction type: " + value.getType());
                        System.out.println("Operand value: " + paralleledMoves.get(i).getTo());
                        System.out.println("Operand type: " + paralleledMoves.get(i).getTo().getType());
                        finalMoves.add(0, temp); // 将临时Move指令添加到最终的Move指令列表中
                        for (int k = j; k <= paralleledMoves.size() - 1; k++) { // 遍历并行的Move指令列表
                            if (var2reg.containsKey(paralleledMoves.get(k).getFrom()) && // 如果源寄存器在寄存器映射中
                                    var2reg.get(paralleledMoves.get(i).getTo()) == var2reg.get(paralleledMoves.get(k).getFrom())) { // 如果目标寄存器和源寄存器相同
                                paralleledMoves.get(k).setFrom(value); // 设置源寄存器为临时变量
                            }
                        }
                    }
                }
                finalMoves.add(paralleledMoves.get(i)); // 将并行的Move指令添加到最终的Move指令列表中
            }
            if (parent.getSuccessors().size() > 1) { // 如果父基本块有多个后继基本块
                BasicBlock newBlock = new BasicBlock(IRBuilder.blockName + curBlock.getParentFunc().getBlockId(), // 创建新的基本块
                        curBlock.getParentFunc());
                List<BasicBlock> blocks = curBlock.getParentFunc().getBasicBlocks(); // 获取函数的基本块列表
                blocks.add(blocks.indexOf(curBlock), newBlock); // 将新的基本块添加到基本块列表中
                for (Instruction instr : finalMoves) { // 遍历最终的Move指令列表
                    newBlock.addInstr(instr); // 将Move指令添加到新的基本块中
                }
                Branch jmpInstr = new Branch(curBlock, newBlock); // 创建跳转指令
                newBlock.addInstr(jmpInstr); // 将跳转指令添加到新的基本块中
                Branch branchInstr = (Branch) parent.getInstrs().get(parent.getInstrs().size() - 1); // 获取父基本块的最后一条指令
                branchInstr.getOperands().set(branchInstr.getOperands().indexOf(curBlock), newBlock); // 将跳转目标设置为新的基本块
            } else {
                for (Instruction instr : finalMoves) { // 遍历最终的Move指令列表
                    parent.getInstrs().add(parent.getInstrs().size() - 1, instr); // 将Move指令添加到父基本块中
                }
            }
        }
    }
}