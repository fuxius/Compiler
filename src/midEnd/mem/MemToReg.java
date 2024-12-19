package midEnd.mem;

import LLVMIR.Ins.Alloca;
import LLVMIR.Base.*;
import LLVMIR.Global.Function;
import LLVMIR.IRBuilder;
import LLVMIR.Ins.Alloca;
import LLVMIR.Ins.Load;
import LLVMIR.Ins.Phi;
import LLVMIR.Ins.Store;

import LLVMIR.Base.Module;
import LLVMIR.LLVMType.LLVMType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class MemToReg {
    private static Alloca curAlloca;
    private static ArrayList<Instruction> defInstrs;
    private static ArrayList<Instruction> useInstrs;
    private static ArrayList<BasicBlock> defBlocks;
    private static ArrayList<BasicBlock> useBlocks;
    private static Stack<Value> defStack;

    public static void execute(Module module) {
        for (Function func : module.getFunctions()) {
            for (BasicBlock block : func.getBasicBlocks()) {
                ArrayList<Instruction> instrs=new ArrayList<>(block.getInstrs());
                for (Instruction instr : instrs) {
                    if (instr instanceof Alloca && (((Alloca) instr).getPointedType() == LLVMType.Int32||((Alloca) instr).getPointedType() == LLVMType.Int8)) {
                        curAlloca = (Alloca) instr;
                        init();
                        insertPhi();
                        rename(func.getBasicBlocks().get(0));
                    }
                }
            }
        }
    }

    public static void init() {
        useBlocks = new ArrayList<>();
        useInstrs = new ArrayList<>();
        defBlocks = new ArrayList<>();
        defInstrs = new ArrayList<>();
        defStack = new Stack<>();
        for (User user : curAlloca.getUsers()) {
            Instruction instr = (Instruction) user;
            if (instr instanceof Load && !instr.getParentBlock().isDeleted()) {
                useInstrs.add(instr);
                if (!useBlocks.contains(instr.getParentBlock())) {
                    useBlocks.add(instr.getParentBlock());
                }
            }
            if (instr instanceof Store && !instr.getParentBlock().isDeleted()) {
                defInstrs.add(instr);
                if (!defBlocks.contains(instr.getParentBlock())) {
                    defBlocks.add(instr.getParentBlock());
                }
            }
        }
    }

    public static void insertPhi() {
        HashSet<BasicBlock> f = new HashSet<>();
        ArrayList<BasicBlock> w = new ArrayList<>(defBlocks);
        while (!w.isEmpty()) {
            BasicBlock x = w.get(0);
            w.remove(0);
            for (BasicBlock y : x.getDF()) {
                if (!f.contains(y)) {
                    insertAtBegin(y);
                    f.add(y);
                    if (!defBlocks.contains(y)) {
                        w.add(y);
                    }
                }
            }
        }
    }

    public static void insertAtBegin(BasicBlock block) {
        // 添加调试信息
        System.out.println("Inserting phi at block: " + block.getName());
        System.out.println("Parent function: " + (block.getParentFunc() != null ? block.getParentFunc().getName() : "null"));
        System.out.println("Parent blocks: " + block.getPredecessors());

        String name = IRBuilder.tempName + block.getParentFunc().getVarId();
        ArrayList<BasicBlock> parents = new ArrayList<>(block.getPredecessors());
        System.out.println("Number of parent blocks: " + parents.size());

        Phi phiInstr = new Phi(name, block, parents,curAlloca.getPointedType());
        block.getInstrs().add(0, phiInstr);
        useInstrs.add(phiInstr);
        defInstrs.add(phiInstr);
    }

    public static void rename(BasicBlock block) {
        System.out.println("=== Start renaming block: " + block.getName() + " ===");
        Iterator<Instruction> it = block.getInstrs().iterator();
        int pushCnt = 0;

        // 打印当前 defStack 的状态
        System.out.println("Current defStack size: " + defStack.size());

        while (it.hasNext()) {
            Instruction instr = it.next();
            System.out.println("Processing instruction: " + instr);

            if (instr == curAlloca) {
                System.out.println("Found curAlloca, removing");
                instr.removeOperands();
                it.remove();
            } else if (instr instanceof Load && useInstrs.contains(instr)) {
                Value newValue = defStack.empty() ? new Undef() : defStack.peek();
                System.out.println("Load instruction, new value: " + newValue);
                instr.modifyValueForUsers(newValue);
                instr.removeOperands();
                it.remove();
            } else if (instr instanceof Store && defInstrs.contains(instr)) {
                Value fromValue = ((Store) instr).getFrom();
                System.out.println("Store instruction, pushing value: " + fromValue);
                defStack.push(fromValue);
                instr.removeOperands();
                pushCnt++;
                it.remove();
            } else if (instr instanceof Phi && defInstrs.contains(instr)) {
                System.out.println("Phi instruction, pushing itself");
                pushCnt++;
                defStack.push(instr);
            }
        }

        System.out.println("Processing children:");
        for (BasicBlock child : block.getSuccessors()) {
            if (!child.getInstrs().isEmpty()) {
                Instruction firstInstr = child.getInstrs().get(0);
                if (firstInstr instanceof Phi && useInstrs.contains(firstInstr)) {
                    Value value = defStack.empty() ? new Undef() : defStack.peek();
                    // 确保永远不会添加 null 值
                    if (value == null) {
                        value = new Undef();
                    }
                    ((Phi) firstInstr).addIncomingValue(block, value);
                }
            }
        }

        System.out.println("Processing imdommed blocks");
        for (BasicBlock imdommed : block.getImdom()) {
            rename(imdommed);
        }

        System.out.println("Popping " + pushCnt + " values from defStack");
        for (int i = 1; i <= pushCnt; i++) {
            defStack.pop();
        }
        System.out.println("=== End renaming block: " + block.getName() + " ===");
    }
}
