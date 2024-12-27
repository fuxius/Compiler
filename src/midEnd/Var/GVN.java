package midEnd.Var;

import LLVMIR.Base.*;
import LLVMIR.Base.Core.User;
import LLVMIR.Base.Core.Value;
import LLVMIR.Base.Core.Module;
import LLVMIR.Global.Function;
import LLVMIR.Global.GlobalVar;
import LLVMIR.IRBuilder;
import LLVMIR.Ins.*;
import LLVMIR.Ins.Mem.Alloca;
import LLVMIR.Ins.Mem.Load;
import midEnd.helper.delete;
import midEnd.Optimizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 全局值编号(Global Value Numbering)优化器
 * 用于消除冗余计算和优化代码
 */
public class GVN {
    // 存储值编号的映射关系
    private static HashMap<String, Value> valueNumberMap = new HashMap<>();
    // 当前处理的基本块
    private static BasicBlock currentBlock;
    // 需要删除的基本块集合
    private static HashSet<BasicBlock> redundantBlocks;

    /**
     * 优化给定的模块
     * @param module 待优化的LLVM IR模块
     */
    public static void optimize(Module module) {
        // 第一遍：全局值编号优化
        performGVNPass(module);
        // 第二遍：代数优化和常量传播
        performAlgebraicPass(module);
        // 清理和简化代码
        delete.simplify(module);
    }

    /**
     * 执行全局值编号优化遍历
     */
    private static void performGVNPass(Module module) {
        for (Function function : module.getFunctions()) {
            valueNumberMap = new HashMap<>();
            processBlockRecursively(function.getBasicBlocks().get(0));
        }
    }

    /**
     * 递归处理基本块及其支配树
     */
    private static void processBlockRecursively(BasicBlock block) {
        ArrayList<Instruction> instructions = new ArrayList<>(block.getInstrs());
        HashSet<String> processedHashes = new HashSet<>();

        for (Instruction instruction : instructions) {
            if (isGVNEligible(instruction)) {
                processInstruction(instruction, block, processedHashes);
            }
        }

        for (BasicBlock dominated : block.getImdom()) {
            processBlockRecursively(dominated);
        }

        for (String hash : processedHashes) {
            valueNumberMap.remove(hash);
        }
    }

    /**
     * 判断指令是否适合进行GVN优化
     */
    private static boolean isGVNEligible(Instruction instruction) {
        return instruction instanceof Alu ||
                instruction instanceof Icmp ||
                (instruction instanceof Call && ((Function) instruction.getOperands().get(0)).isGvnAble()) ||
                instruction instanceof GetPtr;
    }
    /**
     * 处理单个指令的GVN优化
     * @param instruction 待处理的指令
     * @param block 当前基本块
     * @param processedHashes 已处理的哈希值集合
     */
    private static void processInstruction(Instruction instruction, BasicBlock block, HashSet<String> processedHashes) {
        String gvnHash = instruction.getGvnHash();
        if (valueNumberMap.containsKey(gvnHash)) {
            // 发现重复计算，用已有的值替换当前指令
            instruction.modifyValueForUsers(valueNumberMap.get(gvnHash));
            block.getInstrs().remove(instruction);
            instruction.removeOperands();
        } else {
            // 记录新的值编号
            valueNumberMap.put(gvnHash, instruction);
            processedHashes.add(gvnHash);
        }
    }
    /**
     * 将余数运算转换为除法运算
     */
    private static void convertRemainderToDiv(Alu instruction) {
        Value operand1 = instruction.getOperands().get(0);
        Value operand2 = instruction.getOperands().get(1);

        if (!(operand1 instanceof Constant) && (operand2 instanceof Constant)) {
            String functionName = currentBlock.getParentFunc().getVarName();

            // 创建除法指令
            Alu divInstruction = new Alu(functionName, operand1, operand2, Alu.OP.SDIV, currentBlock);
            // 创建乘法指令
            Alu mulInstruction = new Alu(functionName, divInstruction, operand2, Alu.OP.MUL, currentBlock);
            // 创建减法指令
            Alu subInstruction = new Alu(functionName, operand1, mulInstruction, Alu.OP.SUB, currentBlock);

            // 替换和更新指令
            int instructionIndex = currentBlock.getInstrs().indexOf(instruction);
            currentBlock.getInstrs().set(instructionIndex, divInstruction);
            currentBlock.getInstrs().add(instructionIndex + 1, mulInstruction);
            currentBlock.getInstrs().add(instructionIndex + 2, subInstruction);

            instruction.modifyValueForUsers(subInstruction);
            instruction.removeOperands();
            currentBlock.getInstrs().remove(instruction);
        }
    }

    /**
     * 优化分支指令
     */
    private static void optimizeBranchInstruction(Branch branch) {
        if (!Optimizer.againstLlvm) {
            return;
        }

        Value condition = branch.getOperands().get(0);
        if (!(condition instanceof Constant)) {
            return;
        }

        Branch simplifiedBranch;
        if (((Constant) condition).getValue() == 0) {
            simplifiedBranch = new Branch(branch.getElseBlock(), currentBlock);
            currentBlock.deleteForPhi(branch.getThenBlock());
            currentBlock.getSuccessors().remove(branch.getThenBlock());
            branch.getThenBlock().getPredecessors().remove(currentBlock);

            if (branch.getThenBlock().getPredecessors().isEmpty()) {
                redundantBlocks.add(branch.getThenBlock());
            }
        } else {
            simplifiedBranch = new Branch(branch.getThenBlock(), currentBlock);
            currentBlock.deleteForPhi(branch.getElseBlock());
            currentBlock.getSuccessors().remove(branch.getElseBlock());
            branch.getElseBlock().getPredecessors().remove(currentBlock);

            if (branch.getElseBlock().getPredecessors().isEmpty()) {
                redundantBlocks.add(branch.getElseBlock());
            }
        }

        branch.removeOperands();
        currentBlock.getInstrs().set(currentBlock.getInstrs().indexOf(branch), simplifiedBranch);
    }

    /**
     * 优化比较指令
     */
    private static void optimizeCompareInstruction(Icmp icmp) {
        if (Optimizer.basicOptimize) {
            return;
        }

        Value value1 = icmp.getOperands().get(0);
        Value value2 = icmp.getOperands().get(1);

        if (!(value1 instanceof Constant) || !(value2 instanceof Constant)) {
            return;
        }

        int const1 = ((Constant) value1).getValue();
        int const2 = ((Constant) value2).getValue();
        Constant result;

        // 根据比较操作符计算结果
        boolean comparisonResult = switch (icmp.getOp()) {
            case EQ -> const1 == const2;
            case NE -> const1 != const2;
            case SGE -> const1 >= const2;
            case SGT -> const1 > const2;
            case SLE -> const1 <= const2;
            case SLT -> const1 < const2;
        };

        result = new Constant(comparisonResult ? 1 : 0);
        currentBlock.getInstrs().remove(icmp);
        icmp.removeOperands();
        icmp.modifyValueForUsers(result);
    }

    /**
     * 优化零扩展指令
     */
    private static void optimizeZeroExtension(Zext zext) {
        if (!Optimizer.againstLlvm) {
            return;
        }

        Value operand = zext.getOperands().get(0);
        zext.modifyValueForUsers(operand);
        zext.removeOperands();
        currentBlock.getInstrs().remove(zext);
    }

    /**
     * 优化全局变量获取指令
     */
    private static void optimizeGlobalVarAccess(GetPtr instruction) {
        Value basePtr = instruction.getOperands().get(0);
        Value offset = instruction.getOperands().get(1);

        if (basePtr instanceof GlobalVar && ((GlobalVar) basePtr).isConst() && offset instanceof Constant) {
            int index = ((Constant) offset).getValue();
            Constant newValue = new Constant(((GlobalVar) basePtr).getInitial().get(index));

            boolean allLoadsRemoved = true;
            Iterator<User> userIterator = instruction.getUsers().iterator();

            while (userIterator.hasNext()) {
                User user = userIterator.next();
                if (user instanceof Load) {
                    user.modifyValueForUsers(newValue);
                    userIterator.remove();
                    ((Load) user).getParentBlock().getInstrs().remove(user);
                    user.removeOperands();
                } else {
                    allLoadsRemoved = false;
                }
            }

            if (allLoadsRemoved) {
                currentBlock.getInstrs().remove(instruction);
                instruction.removeOperands();
            }
        }
    }

    /**
     * 优化局部常量访问
     */
    private static void optimizeLocalConstAccess(GetPtr getPtr) {
        Value base = getPtr.getOperands().get(0);
        Value offset = getPtr.getOperands().get(1);

        if (base instanceof GetPtr baseGetPtr) {
            Value arrayBase = baseGetPtr.getOperands().get(0);
            if (arrayBase instanceof Alloca alloca &&
                    alloca.isConst() &&
                    offset instanceof Constant offsetConst) {

                ArrayList<Integer> initialValues = alloca.getInitial();
                Constant newValue = new Constant(initialValues.get(offsetConst.getValue()));

                boolean allLoadsRemoved = true;
                Iterator<User> userIterator = getPtr.getUsers().iterator();

                while (userIterator.hasNext()) {
                    User user = userIterator.next();
                    if (user instanceof Load) {
                        user.modifyValueForUsers(newValue);
                        userIterator.remove();
                        ((Load) user).getParentBlock().getInstrs().remove(user);
                        user.removeOperands();
                    } else {
                        allLoadsRemoved = false;
                    }
                }

                if (allLoadsRemoved) {
                    currentBlock.getInstrs().remove(getPtr);
                    getPtr.removeOperands();
                }
            }
        }
    }

    /**
     * 优化算术指令中的常量
     */
    private static void optimizeConstantArithmetic(Alu instruction) {
        Value operand1 = instruction.getOperands().get(0);
        Value operand2 = instruction.getOperands().get(1);
        int constCount = 0;

        // 计算常量操作数的数量
        if (operand1 instanceof Constant) constCount++;
        if (operand2 instanceof Constant) constCount++;

        if (constCount == 2) {
            optimizeTwoConstantsAlu(instruction);
        } else if (constCount == 1) {
            optimizeOneConstantAlu(instruction);
        } else {
            optimizeNonConstantAlu(instruction);
        }
    }

    /**
     * 优化两个常量的算术运算
     */
    private static void optimizeTwoConstantsAlu(Alu instruction) {
        Constant const1 = (Constant) instruction.getOperands().get(0);
        Constant const2 = (Constant) instruction.getOperands().get(1);
        int value1 = const1.getValue();
        int value2 = const2.getValue();

        int result = switch (instruction.getOp()) {
            case ADD -> value1 + value2;
            case MUL -> value1 * value2;
            case SUB -> value1 - value2;
            case SDIV -> value2 != 0 ? value1 / value2 : 0;
            case SREM -> value2 != 0 ? value1 % value2 : 0;
        };

        Constant newConstant = new Constant(result);
        instruction.modifyValueForUsers(newConstant);
        currentBlock.getInstrs().remove(instruction);
        instruction.removeOperands();
    }

    /**
     * 优化一个常量的算术运算
     */
    private static void optimizeOneConstantAlu(Alu instruction) {
        Constant constant;
        Value nonConstant;
        boolean isConstantFirstOperand;

        if (instruction.getOperands().get(0) instanceof Constant) {
            constant = (Constant) instruction.getOperands().get(0);
            nonConstant = instruction.getOperands().get(1);
            isConstantFirstOperand = true;
        } else {
            constant = (Constant) instruction.getOperands().get(1);
            nonConstant = instruction.getOperands().get(0);
            isConstantFirstOperand = false;
        }

        optimizeOneConstantOperation(instruction, constant, nonConstant, isConstantFirstOperand);
    }

    /**
     * 优化特定的一个常量运算
     */
    private static void optimizeOneConstantOperation(Alu instruction, Constant constant,
                                                     Value nonConstant, boolean isConstantFirstOperand) {
        int constValue = constant.getValue();
        Alu.OP operation = instruction.getOp();

        // 处理加法中的0
        if (operation == Alu.OP.ADD && constValue == 0) {
            instruction.modifyValueForUsers(nonConstant);
            currentBlock.getInstrs().remove(instruction);
            instruction.removeOperands();
            return;
        }

        // 处理乘法中的特殊情况
        if (operation == Alu.OP.MUL) {
            handleSpecialMultiplication(instruction, constValue, nonConstant);
            return;
        }

        // 处理除法中的特殊情况
        if (operation == Alu.OP.SDIV) {
            handleSpecialDivision(instruction, constant, nonConstant, isConstantFirstOperand);
        }
    }
    /**
     * 处理乘法中的特殊情况
     */
    private static void handleSpecialMultiplication(Alu instruction, int constValue, Value nonConstant) {
        if (constValue == 0) {
            // 乘0得0
            instruction.modifyValueForUsers(new Constant(0));
            currentBlock.getInstrs().remove(instruction);
            instruction.removeOperands();
        } else if (constValue == 1) {
            // 乘1不变
            instruction.modifyValueForUsers(nonConstant);
            currentBlock.getInstrs().remove(instruction);
            instruction.removeOperands();
        } else if (constValue == -1) {
            // 乘-1等于取反
            Alu negation = new Alu(currentBlock.getParentFunc().getVarName(),
                    new Constant(0), nonConstant, Alu.OP.SUB, currentBlock);
            instruction.modifyValueForUsers(negation);
            currentBlock.getInstrs().set(currentBlock.getInstrs().indexOf(instruction), negation);
            instruction.removeOperands();
        } else if (constValue >= -4 && constValue <= 5 && Optimizer.basicOptimize) {
            // 对于小的常数，使用加法替代乘法
            optimizeSmallMultiplication(instruction, constValue, nonConstant);
        }
    }

    /**
     * 优化小常数的乘法运算
     */
    private static void optimizeSmallMultiplication(Alu instruction, int constValue, Value nonConstant) {
        String tempVarName = IRBuilder.tempName + currentBlock.getParentFunc().getVarId();
        Alu currentAlu = new Alu(tempVarName, nonConstant, nonConstant, Alu.OP.ADD, currentBlock);
        currentBlock.getInstrs().add(currentBlock.getInstrs().indexOf(instruction), currentAlu);

        // 通过重复加法实现乘法
        for (int i = 1; i <= Math.abs(constValue) - 2; i++) {
            currentAlu = new Alu(tempVarName, currentAlu, nonConstant, Alu.OP.ADD, currentBlock);
            currentBlock.getInstrs().add(currentBlock.getInstrs().indexOf(instruction), currentAlu);
        }

        // 处理负数情况
        if (constValue < 0) {
            currentAlu = new Alu(tempVarName, new Constant(0), currentAlu, Alu.OP.SUB, currentBlock);
            currentBlock.getInstrs().add(currentBlock.getInstrs().indexOf(instruction), currentAlu);
        }

        instruction.modifyValueForUsers(currentAlu);
        currentBlock.getInstrs().remove(instruction);
        instruction.removeOperands();
    }

    /**
     * 处理除法中的特殊情况
     */
    private static void handleSpecialDivision(Alu instruction, Constant constant,
                                              Value nonConstant, boolean isConstantFirstOperand) {
        int constValue = constant.getValue();

        if (constValue == 0 && isConstantFirstOperand) {
            // 0除以任何数等于0
            instruction.modifyValueForUsers(new Constant(0));
            currentBlock.getInstrs().remove(instruction);
            instruction.removeOperands();
        } else if (constValue == 1 && !isConstantFirstOperand) {
            // 任何数除以1等于其本身
            instruction.modifyValueForUsers(nonConstant);
            currentBlock.getInstrs().remove(instruction);
            instruction.removeOperands();
        } else if (constValue == -1 && !isConstantFirstOperand) {
            // 除以-1等于取反
            Alu negation = new Alu(currentBlock.getParentFunc().getVarName(),
                    new Constant(0), nonConstant, Alu.OP.SUB, currentBlock);
            instruction.modifyValueForUsers(negation);
            currentBlock.getInstrs().set(currentBlock.getInstrs().indexOf(instruction), negation);
            instruction.removeOperands();
        }
    }

    /**
     * 优化没有常量的算术运算
     */
    private static void optimizeNonConstantAlu(Alu instruction) {
        Value operand1 = instruction.getOperands().get(0);
        Value operand2 = instruction.getOperands().get(1);

        // 如果两个操作数相同
        if (operand1.getName().equals(operand2.getName())) {
            switch (instruction.getOp()) {
                case SUB -> {
                    // x - x = 0
                    instruction.modifyValueForUsers(new Constant(0));
                    currentBlock.getInstrs().remove(instruction);
                    instruction.removeOperands();
                }
                case SDIV -> {
                    // x / x = 1
                    instruction.modifyValueForUsers(new Constant(1));
                    currentBlock.getInstrs().remove(instruction);
                    instruction.removeOperands();
                }
                case SREM -> {
                    // x % x = 0
                    instruction.modifyValueForUsers(new Constant(0));
                    currentBlock.getInstrs().remove(instruction);
                    instruction.removeOperands();
                }
            }
        }
    }

    /**
     * 执行代数优化和常量传播
     */
    private static void performAlgebraicPass(Module module) {
        for (Function function : module.getFunctions()) {
            redundantBlocks = new HashSet<>();

            for (BasicBlock block : function.getBasicBlocks()) {
                currentBlock = block;
                ArrayList<Instruction> instructions = new ArrayList<>(block.getInstrs());

                // 处理余数转除法
                for (Instruction instruction : instructions) {
                    if (instruction instanceof Alu aluInstruction &&
                            aluInstruction.getOp() == Alu.OP.SREM) {
                        convertRemainderToDiv(aluInstruction);
                    }
                }

                // 主要优化遍历
                instructions = new ArrayList<>(block.getInstrs());
                for (Instruction instruction : instructions) {
                    if (instruction instanceof Alu) {
                        optimizeConstantArithmetic((Alu) instruction);
                    } else if (instruction instanceof Icmp) {
                        optimizeCompareInstruction((Icmp) instruction);
                    } else if (instruction instanceof Branch) {
                        optimizeBranchInstruction((Branch) instruction);
                    } else if (instruction instanceof GetPtr getPtr) {
                        optimizeGlobalVarAccess(getPtr);
                        optimizeLocalConstAccess(getPtr);
                    } else if (instruction instanceof Zext) {
                        optimizeZeroExtension((Zext) instruction);
                    }
                }

                // 聚合算术优化遍历
                instructions = new ArrayList<>(block.getInstrs());
                for (Instruction instruction : instructions) {
                    if (instruction instanceof Alu) {
                        optimizeConstantArithmetic((Alu) instruction);
                    }
                }
            }

            // 清理冗余基本块
            for (BasicBlock block : redundantBlocks) {
                block.setDeleted();
                for (Instruction instruction : block.getInstrs()) {
                    instruction.removeOperands();
                }
            }
            function.getBasicBlocks().removeIf(redundantBlocks::contains);
        }
    }
}

//这个全局值编号(GVN)优化器的主要思路是：
//通过识别等价计算来消除冗余操作，同时结合多种代数优化和常量传播技术。整体分为三个主要部分：
//
//全局值编号(GVN)核心优化：
//
//
//为每个指令计算唯一的哈希值，代表其计算本质
//维护值编号映射表，记录已见过的计算
//当发现相同哈希值的指令时，用已有的结果替换当前计算
//按支配树顺序处理，确保正确性
//
//
//代数优化和常量传播：
//
//
//处理常量计算：折叠纯常量运算
//代数化简：
//
//处理乘0、乘1、加0等特殊情况
//优化小常数乘法为加法序列
//识别x-x=0, x/x=1等模式
//
//
//余数运算优化：将%转换为更高效的/操作
//分支优化：化简常量条件的分支
//类型转换优化：消除不必要的零扩展
//
//
//内存访问优化：
//
//
//处理全局常量数组访问
//优化局部常量数组访问
//将数组访问替换为直接值引用
//
//这些优化的目标是：
//
//消除重复计算
//减少运算强度
//简化控制流
//提前计算常量表达式
//降低内存访问开销
//
//整体设计采用了多遍优化策略，每一遍专注于特定类型的优化机会，最后通过死代码删除来清理优化后的残留代码。