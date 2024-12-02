package midEnd;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.Base.Value;
import LLVMIR.Global.Function;
import LLVMIR.Base.Module;
import LLVMIR.Ins.Alloca;
import LLVMIR.Ins.Call;
import backEnd.Base.Register;

import java.util.*;

/**
 * 寄存器分配器类，基于线性扫描算法并结合活跃变量分析
 */
public class RegAlloc {
    private final Set<Register> availableRegisters; // 可用寄存器集合
    private final Map<Value, Register> varToRegMap; // 变量到寄存器的映射
    private final Map<Register, Value> regToVarMap; // 寄存器到变量的映射
    private final Map<Value, Integer> useCountMap; // 变量的使用计数
    private Function currentFunction; // 当前处理的函数

    /**
     * 构造函数，初始化寄存器集合和映射
     */
    public RegAlloc() {
        this.availableRegisters = new HashSet<>();
        // 添加 $t0 到 $t9
        availableRegisters.add(Register.T0);
        availableRegisters.add(Register.T1);
        availableRegisters.add(Register.T2);
        availableRegisters.add(Register.T3);
        availableRegisters.add(Register.T4);
        availableRegisters.add(Register.T5);
        availableRegisters.add(Register.T6);
        availableRegisters.add(Register.T7);
        availableRegisters.add(Register.T8);
        availableRegisters.add(Register.T9);
        // 添加 $s0 到 $s7
        availableRegisters.add(Register.S0);
        availableRegisters.add(Register.S1);
        availableRegisters.add(Register.S2);
        availableRegisters.add(Register.S3);
        availableRegisters.add(Register.S4);
        availableRegisters.add(Register.S5);
        availableRegisters.add(Register.S6);
        availableRegisters.add(Register.S7);
        this.varToRegMap = new HashMap<>();
        this.regToVarMap = new HashMap<>();
        this.useCountMap = new HashMap<>();
    }

    /**
     * 为模块中的每个函数分配寄存器
     *
     * @param module 要处理的模块
     */
    public void allocateRegisters(Module module) {
        for (Function func : module.getFunctions()) { // 遍历模块中的每个函数
            resetState(); // 重置映射和计数
            this.currentFunction = func; // 设置当前函数
            initializeUseCounts(); // 初始化使用计数
            performRegisterAllocation(); // 执行寄存器分配
            annotateActiveRegs(); // 标注活跃寄存器
            func.setRegisterPool(new HashMap<>(varToRegMap)); // 设置函数的变量到寄存器的映射
            printRegisterAllocation(func); // 打印寄存器分配结果
        }
    }

    /**
     * 重置寄存器分配器的状态
     */
    private void resetState() {
        varToRegMap.clear();
        regToVarMap.clear();
        useCountMap.clear();
    }

    /**
     * 初始化变量的使用计数
     */
    private void initializeUseCounts() {
        for (BasicBlock block : currentFunction.getBasicBlocks()) { // 遍历函数中的每个基本块
            for (Instruction instr : block.getInstrs()) { // 遍历基本块中的每条指令
                for (Value operand : instr.getOperands()) { // 遍历指令的操作数
                    useCountMap.put(operand, useCountMap.getOrDefault(operand, 0) + 1);
                }
                if (instr.hasLVal()) { // 如果指令有左值
                    useCountMap.put(instr, useCountMap.getOrDefault(instr, 0) + 1);
                }
            }
        }
    }

    /**
     * 执行寄存器分配
     */
    private void performRegisterAllocation() {
        // 按照支配树的顺序遍历基本块
        visitBlock(currentFunction.getBasicBlocks().get(0)); // 访问并处理基本块
    }

    /**
     * 访问并处理基本块，进行寄存器分配和释放
     *
     * @param entry 当前基本块
     */
    private void visitBlock(BasicBlock entry) {
        List<Instruction> instrs = entry.getInstrs(); // 获取基本块的指令列表
        HashSet<Value> localDefed = new HashSet<>(); // 初始化局部定义的变量集合
        HashMap<Value, Instruction> lastUse = new HashMap<>(); // 初始化最后使用的指令映射
        HashSet<Value> neverUsed = new HashSet<>(); // 初始化从未使用的变量集合

        // 第一步：确定每个变量的最后使用指令
        for (Instruction instr : instrs) {
            for (Value value : instr.getOperands()) {
                lastUse.put(value, instr); // 更新最后使用的指令
            }
        }

        // 第二步：遍历指令，释放不再活跃的寄存器，并进行寄存器分配
        for (Instruction instr : instrs) {
            for (Value operand : instr.getOperands()) {
                // 判断是否是最后使用，且不在Out集合中，且已分配寄存器
                if (lastUse.get(operand) == instr && !entry.getOutSet().contains(operand) && varToRegMap.containsKey(operand)) {
                    Register reg = varToRegMap.get(operand);
                    regToVarMap.remove(reg); // 移除寄存器映射
                    neverUsed.add(operand); // 记录为不再使用
                }
            }

            // 对有左值的指令进行寄存器分配
            if (instr.hasLVal() && !(instr instanceof Alloca && instr.getType().isArray())) {
                localDefed.add(instr); // 记录局部定义
                allocateRegister(instr, entry); // 尝试分配寄存器
            }
        }

        // 第三步：递归处理直接支配的子基本块
        for (BasicBlock block : entry.getImdom()) { // 遍历直接支配的基本块
            HashMap<Register, Value> curChildNeverUse = new HashMap<>(); // 初始化当前子基本块从未使用的映射

            for (Register reg : regToVarMap.keySet()) { // 遍历寄存器到变量的映射
                Value var = regToVarMap.get(reg);
                if (!block.getInSet().contains(var)) { // 如果寄存器对应的变量不在子基本块的In集合中
                    curChildNeverUse.put(reg, var); // 记录为不再使用
                }
            }

            // 释放这些寄存器
            for (Register reg : curChildNeverUse.keySet()) {
                regToVarMap.remove(reg); // 移除寄存器到变量的映射
            }

            // 递归访问子基本块
            visitBlock(block);

            // 恢复寄存器映射
            for (Map.Entry<Register, Value> entryNeverUse : curChildNeverUse.entrySet()) {
                regToVarMap.put(entryNeverUse.getKey(), entryNeverUse.getValue()); // 恢复寄存器到变量的映射
            }
        }

        // 第四步：清理局部定义的寄存器映射
        for (Value value : localDefed) {
            if (varToRegMap.containsKey(value)) {
                Register reg = varToRegMap.get(value);
                regToVarMap.remove(reg); // 移除寄存器到变量的映射
            }
        }

        // 第五步：恢复未使用变量的寄存器映射
        for (Value value : neverUsed) {
            if (!localDefed.contains(value) && varToRegMap.containsKey(value)) {
                Register reg = varToRegMap.get(value);
                regToVarMap.put(reg, value);
            }
        }
    }

    /**
     * 为变量分配寄存器
     *
     * @param value 要分配寄存器的变量
     * @param block 当前变量所在的基本块
     */
    private void allocateRegister(Value value, BasicBlock block) {
        if (varToRegMap.containsKey(value)) {
            // 变量已经分配了寄存器
            return;
        }

        // 尝试分配一个可用的寄存器
        for (Register reg : availableRegisters) {
            if (!regToVarMap.containsKey(reg)) {
                assignRegister(value, reg); // 分配寄存器
                return;
            }
        }

        // 如果没有可用的寄存器，则选择一个寄存器进行溢出
        Register regToSpill = selectRegisterToSpill(block); // 传入当前基本块的 Out 集合
        if (regToSpill != null) {
            if(regToVarMap.containsKey(regToSpill)) {
                Value spilledVar = regToVarMap.get(regToSpill);
                varToRegMap.remove(spilledVar); // 从寄存器到变量的映射中移除
            }
            assignRegister(value, regToSpill);
            // 保留寄存器分配给新变量
        }
    }

    /**
     * 分配寄存器并更新映射
     *
     * @param value 变量
     * @param reg   寄存器
     */
    private void assignRegister(Value value, Register reg) {
        varToRegMap.put(value, reg);
        regToVarMap.put(reg, value);
    }

    /**
     * 选择一个寄存器进行溢出（优先选择不在当前块 Out 集合中的变量，使用计数最低的变量）
     *
     * @param currentBlock 当前正在处理的基本块
     * @return 要溢出的寄存器，若无则返回 null
     */
    private Register selectRegisterToSpill(BasicBlock currentBlock) {
        Register regToSpill = null;
        int minUseCount = Integer.MAX_VALUE;

        // 第一轮：选择不在 Out 集合中的变量，并且使用计数最低的变量
        for (Map.Entry<Register, Value> entry : regToVarMap.entrySet()) {
            Register reg = entry.getKey();
            Value var = entry.getValue();

            // 检查变量是否在当前块的 Out 集合中活跃
            boolean isLive = currentBlock.getOutSet().contains(var);

            if (!isLive) { // 优先选择不再活跃的变量
                int count = useCountMap.getOrDefault(var, 0);
                if (count < minUseCount) {
                    minUseCount = count;
                    regToSpill = reg;
                }
            }
        }

        // 如果没有找到不在 Out 集合中的变量，选择使用计数最低的变量进行溢出
        if (regToSpill == null) {
            for (Map.Entry<Register, Value> entry : regToVarMap.entrySet()) {
                Register reg = entry.getKey();
                Value var = entry.getValue();
                int count = useCountMap.getOrDefault(var, 0);
                if (count < minUseCount) {
                    minUseCount = count;
                    regToSpill = reg;
                }
            }
        }

        return regToSpill;
    }

    /**
     * 标注调用指令的活跃寄存器
     */
    private void annotateActiveRegs() {
        for (BasicBlock block : currentFunction.getBasicBlocks()) { // 遍历基本块
            for (Instruction instr : block.getInstrs()) { // 遍历指令
                if (instr instanceof Call call) { // 如果指令是调用指令
                    HashSet<Register> activeRegs = new HashSet<>();
                    for (Value value : block.getOutSet()) { // 遍历基本块的 Out 集合
                        if (varToRegMap.containsKey(value)) {
                            activeRegs.add(varToRegMap.get(value));
                        }
                    }
                    // 遍历调用指令后的指令，标注活跃寄存器
                    int instrIndex = block.getInstrs().indexOf(call);
                    for (int i = instrIndex + 1; i < block.getInstrs().size(); i++) {
                        Instruction subsequentInstr = block.getInstrs().get(i);
                        for (Value operand : subsequentInstr.getOperands()) {
                            if (varToRegMap.containsKey(operand)) {
                                activeRegs.add(varToRegMap.get(operand));
                            }
                        }
                    }
                    call.setActiveRegs(activeRegs); // 设置调用指令的活跃寄存器集合
                }
            }
        }
    }

    /**
     * 打印寄存器分配结果
     *
     * @param func 当前函数
     */
    private void printRegisterAllocation(Function func) {
        System.out.println("函数: " + func.getName() + " 的寄存器分配结果:");
        if (varToRegMap.isEmpty()) { // 如果映射为空
            System.out.println("  无变量分配到寄存器");
        } else {
            for (Map.Entry<Value, Register> entry : varToRegMap.entrySet()) { // 遍历映射
                System.out.println("  变量: " + entry.getKey().getName() + " -> 寄存器: " + entry.getValue());
            }
        }
        System.out.println();
    }

}