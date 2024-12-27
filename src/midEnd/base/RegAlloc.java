package midEnd.base;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.Base.Core.Value;
import LLVMIR.Global.Function;
import LLVMIR.Base.Core.Module;
import LLVMIR.Ins.Mem.Alloca;
import LLVMIR.Ins.Call;
import LLVMIR.Ins.Mem.Phi;
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
                    useCountMap.put(operand, useCountMap.getOrDefault(operand, 0) + 1+ 10000 * block.getLoopDepth());
                }
                if (instr.hasLVal()) { // 如果指令有左值
                    useCountMap.put(instr, useCountMap.getOrDefault(instr, 0) + 1 + 10000 * block.getLoopDepth());
                }
            }
        }
    }

    /**
     * 执行寄存器分配
     */
    private void performRegisterAllocation() {
        // 按照支配树的顺序遍历基本块
        traverseBlock(currentFunction.getBasicBlocks().get(0)); // 访问并处理基本块
    }

    /**
     * 访问并处理基本块，进行寄存器分配和释放
     *
     * @param currentBlock 当前基本块
     */
    private void traverseBlock(BasicBlock currentBlock) {
        List<Instruction> instructions = currentBlock.getInstrs(); // 获取当前基本块的指令列表
        Set<Value> localDefinitions = new HashSet<>(); // 当前块中局部定义的变量集合
        Map<Value, Instruction> lastUsageMap = new HashMap<>(); // 记录变量的最后使用指令
        Set<Value> variablesNoLongerUsed = new HashSet<>(); // 不再使用的变量集合

        // 第一步：确定每个变量的最后使用指令
        for (Instruction instruction : instructions) {
            for (Value operand : instruction.getOperands()) {
                lastUsageMap.put(operand, instruction); // 更新变量的最后使用指令
            }
        }

        // 第二步：遍历指令，释放不再活跃的寄存器，并进行寄存器分配
        for (Instruction instruction : instructions) {
            if (!(instruction instanceof Phi)) {
                for (Value operand : instruction.getOperands()) {
                    // 如果是变量的最后一次使用，且不在后继块的活跃集合中，且已分配寄存器
                    if (lastUsageMap.get(operand) == instruction
                            && !currentBlock.getOutSet().contains(operand)
                            && varToRegMap.containsKey(operand)) {
                        Register reg = varToRegMap.get(operand);
                        regToVarMap.remove(reg); // 移除寄存器到变量的映射
                        variablesNoLongerUsed.add(operand); // 标记变量不再使用
                    }
                }
            }

            // 对有左值的指令进行寄存器分配
            if (instruction.hasLVal() && !(instruction instanceof Alloca && instruction.getType().isArray())) {
                localDefinitions.add(instruction); // 记录局部定义的变量
                allocateRegister(instruction, currentBlock); // 分配寄存器
            }
        }

        // 第三步：递归处理直接支配的子基本块
        for (BasicBlock childBlock : currentBlock.getImdom()) {
            Map<Register, Value> temporarilyRemovedMappings = new HashMap<>(); // 临时移除的寄存器映射

            // 移除在子块中不活跃的变量的寄存器映射
            for (Map.Entry<Register, Value> entry : regToVarMap.entrySet()) {
                Register reg = entry.getKey();
                Value var = entry.getValue();
                if (!childBlock.getInSet().contains(var)) {
                    temporarilyRemovedMappings.put(reg, var);
                }
            }
            // 从寄存器映射中移除这些变量
            for (Register reg : temporarilyRemovedMappings.keySet()) {
                regToVarMap.remove(reg);
            }

            // 递归访问子基本块
            traverseBlock(childBlock);

            // 恢复寄存器映射
            regToVarMap.putAll(temporarilyRemovedMappings);
        }

        // 第四步：清理局部定义的寄存器映射
        for (Value var : localDefinitions) {
            if (varToRegMap.containsKey(var)) {
                Register reg = varToRegMap.get(var);
                regToVarMap.remove(reg); // 移除寄存器到变量的映射
            }
        }

        // 第五步：恢复不再使用但仍需保留的变量的寄存器映射
        for (Value var : variablesNoLongerUsed) {
            if (!localDefinitions.contains(var) && varToRegMap.containsKey(var)) {
                Register reg = varToRegMap.get(var);
                regToVarMap.put(reg, var); // 恢复寄存器到变量的映射
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

            for (Map.Entry<Register, Value> entry : regToVarMap.entrySet()) {
                Register reg = entry.getKey();
                Value var = entry.getValue();
                int count = useCountMap.getOrDefault(var, 0);
                if (count < minUseCount) {
                    minUseCount = count;
                    regToSpill = reg;
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

//这是一个基于线性扫描算法并结合活跃变量分析的寄存器分配策略。其工作原理：
//
//1. **寄存器资源管理**
//   - 可用寄存器包含：$t0-$t9 (10个临时寄存器) 和 $s0-$s7 (8个保存寄存器)
//   - 使用三个主要映射维护寄存器状态：
//     - varToRegMap: 变量到寄存器的映射
//     - regToVarMap: 寄存器到变量的映射
//     - useCountMap: 变量使用次数的统计
//
//2. **使用计数初始化**
//   - 统计每个变量的使用频率
//   - 考虑循环嵌套深度的影响：`useCount = 基础次数 + 10000 * 循环深度`
//   - 这确保了循环中的变量更可能获得寄存器
//
//3. **寄存器分配过程**
//   - 按照支配树顺序遍历基本块
//   - 对每个基本块的处理分为五个步骤：
//     a. 记录变量最后使用位置
//     b. 释放不再活跃的寄存器
//     c. 递归处理子基本块
//     d. 清理局部定义
//     e. 恢复必要的寄存器映射
//
//4. **寄存器分配策略**
//   当需要为变量分配寄存器时：
//   - 首先查找空闲寄存器
//   - 如果没有空闲寄存器，需要进行溢出选择：
//     1. 优先选择不在当前块Out集合中的变量
//     2. 在相同条件下，选择使用计数最低的变量
//
//5. **特殊处理**
//   - 对数组类型的Alloca指令不分配寄存器
//   - 对函数调用指令(Call)标注活跃寄存器信息
//   - 维护基本块间的变量生命周期
//
//6. **寄存器溢出优化**
//   - 溢出决策考虑两个关键因素：
//     1. 变量是否在当前基本块的Out集合中
//     2. 变量的使用频率（包含循环权重）
//
//这种分配策略的主要优点：
//1. 考虑了循环嵌套对变量重要性的影响
//2. 通过活跃性分析减少不必要的寄存器保存和恢复
//3. 支持跨基本块的寄存器分配
//4. 对函数调用点的寄存器使用进行了优化
//
//这种策略在保证正确性的同时，也在试图优化寄存器的使用效率，特别是对循环中频繁使用的变量给予了更高的优先级。
//在现有代码中我们采用的是基于活跃变量分析的线性扫描算法，而不是图着色算法
//如果要实现冲突图构建，需要以下步骤：
//
//通过基本块的 IN/OUT 集合和变量定义点分析变量的生命周期重叠
//当两个变量的生命周期重叠时，在冲突图中添加一条边
//对于函数调用点，所有跨调用点活跃的变量之间都需要添加冲突边

//关于溢出变量的栈位置管理：
//
//
//在 MipsBuilder 中，我们使用 stackOffset 映射维护变量的栈偏移量
//通过 decreaseStackOffset() 方法为溢出变量分配栈空间
//在需要访问溢出变量时，使用 getStackOffset() 方法获取其偏移量
//所有栈访问都通过 sp 寄存器加偏移量实现

//全局寄存器管理：
//
//
//在现有实现中主要使用：
//
//t0-t9: 临时寄存器
//s0-s7: 保存寄存器
//
//
//函数调用时的寄存器保护：
//
//annotateActiveRegs() 方法标注每个调用点的活跃寄存器
//Call 指令执行前保存这些寄存器到栈上
//Call 返回后从栈上恢复这些寄存器