package backEnd;

import LLVMIR.Base.*;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;
import LLVMIR.Base.Instruction;
import backEnd.Base.Register;

import java.util.*;

public class RegisterAllocator {
    // 活跃变量分析所需的数据结构
    private Map<BasicBlock, Set<Value>> liveIn = new HashMap<>();
    private Map<BasicBlock, Set<Value>> liveOut = new HashMap<>();
    private Map<BasicBlock, Set<Value>> use = new HashMap<>();
    private Map<BasicBlock, Set<Value>> def = new HashMap<>();
    private Map<Instruction, Set<Value>> liveBeforeInstr = new HashMap<>();
    private Map<Instruction, Set<Value>> liveAfterInstr = new HashMap<>();

    // 寄存器池
    private HashMap<Value, Register> registerPool = new HashMap<>();
    // 栈帧偏移量
    private HashMap<Value, Integer> stackOffset = new HashMap<>();
    // 当前栈偏移量
    private int currentStackOffset = 0;

    public RegisterAllocator() {
        // 初始化
    }

    // 调用活跃变量分析和寄存器分配
    public void allocateRegisters(Module module) {
        for (Function function : module.getFunctions()) {
            computeLiveness(function);
            allocateRegisters(function);
        }
    }

    public HashMap<Value, Register> getRegisterPool() {
        return registerPool;
    }

    public HashMap<Value, Integer> getStackOffset() {
        return stackOffset;
    }

    public int getCurrentStackOffset() {
        return currentStackOffset;
    }

    public void decreaseStackOffset(int size) {
        currentStackOffset -= size;
        assert currentStackOffset >= 0;
    }

    // 活跃变量分析
    public void computeLiveness(Function function) {
        // 1. 初始化 use 和 def 集合
        for (BasicBlock bb : function.getBasicBlocks()) {
            Set<Value> useSet = new HashSet<>();
            Set<Value> defSet = new HashSet<>();
            for (Instruction instr : bb.getInstrs()) {
                // 如果指令定义了一个值
                if (instr.hasLVal()) {
                    defSet.add(instr);
                }
                // 对于每个操作数
                for (Value operand : instr.getOperands()) {
                    if (!defSet.contains(operand) && !(operand instanceof Constant)) {
                        useSet.add(operand);
                    }
                }
            }
            use.put(bb, useSet);
            def.put(bb, defSet);
        }

        // 2. 初始化 liveIn 和 liveOut 集合
        for (BasicBlock bb : function.getBasicBlocks()) {
            liveIn.put(bb, new HashSet<>());
            liveOut.put(bb, new HashSet<>());
        }

        // 3. 迭代计算 liveIn 和 liveOut
        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock bb : function.getBasicBlocks()) {
                Set<Value> newLiveOut = new HashSet<>();
                for (BasicBlock succ : bb.getSuccessors()) {
                    newLiveOut.addAll(liveIn.get(succ));
                }
                Set<Value> newLiveIn = new HashSet<>(use.get(bb));
                Set<Value> liveOutMinusDef = new HashSet<>(newLiveOut);
                liveOutMinusDef.removeAll(def.get(bb));
                newLiveIn.addAll(liveOutMinusDef);

                if (!liveIn.get(bb).equals(newLiveIn)) {
                    liveIn.put(bb, newLiveIn);
                    changed = true;
                }
                if (!liveOut.get(bb).equals(newLiveOut)) {
                    liveOut.put(bb, newLiveOut);
                    changed = true;
                }
            }
        }

        // 4. 计算每条指令的活跃变量
        for (BasicBlock bb : function.getBasicBlocks()) {
            Set<Value> live = new HashSet<>(liveOut.get(bb));
            List<Instruction> instrs = bb.getInstrs();
            for (int i = instrs.size() - 1; i >= 0; i--) {
                Instruction instr = instrs.get(i);
                liveAfterInstr.put(instr, new HashSet<>(live));
                if (instr.hasLVal()) {
                    live.remove(instr); // 删除定义的变量
                }
                for (Value operand : instr.getOperands()) {
                    if (!(operand instanceof Constant)) {
                        live.add(operand); // 添加使用的变量
                    }
                }
                liveBeforeInstr.put(instr, new HashSet<>(live));
            }
        }
    }

    // 寄存器分配
    public void allocateRegisters(Function function) {
        // 可用的寄存器列表
        List<Register> allRegisters = Arrays.asList(
                Register.T0, Register.T1, Register.T2, Register.T3, Register.T4,
                Register.T5, Register.T6, Register.T7, Register.T8, Register.T9,
                Register.S0, Register.S1, Register.S2, Register.S3, Register.S4,
                Register.S5, Register.S6, Register.S7
        );
        Set<Register> availableRegisters = new HashSet<>(allRegisters);
        Map<Register, Value> registerAssignment = new HashMap<>();

        // 遍历函数的每个基本块
        for (BasicBlock bb : function.getBasicBlocks()) {
            List<Instruction> instructions = bb.getInstrs();
            for (int i = 0; i < instructions.size(); i++) {
                Instruction instr = instructions.get(i);
                Set<Value> liveAfter = liveAfterInstr.get(instr);
                Set<Value> liveBefore = liveBeforeInstr.get(instr);

                // 处理操作数（使用）
                for (Value operand : instr.getOperands()) {
                    if (!registerPool.containsKey(operand) && !(operand instanceof Constant)) {
                        if (!availableRegisters.isEmpty()) {
                            Register reg = availableRegisters.iterator().next();
                            availableRegisters.remove(reg);
                            registerPool.put(operand, reg);
                            registerAssignment.put(reg, operand);
                        } else {
                            // 寄存器不足，需要溢出
                            Register regToSpill = null;
                            for (Register reg : allRegisters) {
                                Value val = registerAssignment.get(reg);
                                if (!liveBefore.contains(val) && !liveAfter.contains(val)) {
                                    regToSpill = reg;
                                    break;
                                }
                            }
                            if (regToSpill == null) {
                                regToSpill = allRegisters.get(0);
                            }
                            Value valToSpill = registerAssignment.get(regToSpill);
                            if (!stackOffset.containsKey(valToSpill)) {
                                decreaseStackOffset(4);
                                stackOffset.put(valToSpill, getCurrentStackOffset());
                            }
                            // 在生成代码阶段，需要在此处生成存储指令
                            registerPool.remove(valToSpill);
                            registerAssignment.remove(regToSpill);
                            registerPool.put(operand, regToSpill);
                            registerAssignment.put(regToSpill, operand);
                        }
                    }
                }

                // 处理指令的结果（定义）
                if (instr.hasLVal()) {
                    if (!registerPool.containsKey(instr)) {
                        if (!availableRegisters.isEmpty()) {
                            Register reg = availableRegisters.iterator().next();
                            availableRegisters.remove(reg);
                            registerPool.put(instr, reg);
                            registerAssignment.put(reg, instr);
                        } else {
                            // 寄存器不足，需要溢出
                            Register regToSpill = null;
                            for (Register reg : allRegisters) {
                                Value val = registerAssignment.get(reg);
                                if (!liveAfter.contains(val)) {
                                    regToSpill = reg;
                                    break;
                                }
                            }
                            if (regToSpill == null) {
                                regToSpill = allRegisters.get(0);
                            }
                            Value valToSpill = registerAssignment.get(regToSpill);
                            if (!stackOffset.containsKey(valToSpill)) {
                                decreaseStackOffset(4);
                                stackOffset.put(valToSpill, getCurrentStackOffset());
                            }
                            // 在生成代码阶段，需要在此处生成存储指令
                            registerPool.remove(valToSpill);
                            registerAssignment.remove(regToSpill);
                            registerPool.put(instr, regToSpill);
                            registerAssignment.put(regToSpill, instr);
                        }
                    }
                }

                // 在指令之后，释放不再活跃的变量所占用的寄存器
                for (Iterator<Map.Entry<Register, Value>> it = registerAssignment.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Register, Value> entry = it.next();
                    Value val = entry.getValue();
                    if (!liveAfter.contains(val)) {
                        Register reg = entry.getKey();
                        availableRegisters.add(reg);
                        registerPool.remove(val);
                        it.remove();
                    }
                }
            }
        }
    }
}
