package backEnd;

import LLVMIR.Base.*;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;
import LLVMIR.LLVMType.ArrayType;
import LLVMIR.LLVMType.LLVMType;
import backEnd.Base.AsmInstruction;
import backEnd.Base.LableAsm;
import backEnd.Base.Register;
import backEnd.Global.*;
import LLVMIR.Ins.*;
import LLVMIR.Global.ConstStr;
import LLVMIR.Global.GlobalVar;
import backEnd.Instruction.*;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MipsBuilder {
    // 单例模式
    private static MipsBuilder instance = new MipsBuilder();
    // 标识现在的函数
    private Function currentFunction = null;
    // 跟踪当前处理的栈偏移量
    private int currentStackOffset = 0;
    // 使用 HashMap 实现寄存器池
    private HashMap<Value, Register> registerPool = new HashMap<>();
    // 使用 HashMap 实现栈帧偏移量
    private HashMap<Value, Integer> stackOffset = new HashMap<>();
    // 记录已加载的立即数 imm
    private Map<Integer, Register> immCache = new HashMap<>();
    // 是否处于 main 函数中
    private boolean inMain = false;
    // .data 部分，存放数据，使用 GlobalAsm 类型数组存放
    private ArrayList<GlobalAsm> data = new ArrayList<>();
    // .text 部分，存放指令，使用 AsmInstruction 类型数组存放
    private ArrayList<AsmInstruction> text = new ArrayList<>();

    public static MipsBuilder getInstance() {
        return instance;
    }

    public void setCurrentFunction(Function currentFunction) {
        this.currentFunction = currentFunction;
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    public void setCurrentStackOffset(int currentStackOffset) {
        this.currentStackOffset = currentStackOffset;
    }

    public int getCurrentStackOffset() {
        return currentStackOffset;
    }

    public void setInMain(boolean inMain) {
        this.inMain = inMain;
    }

    public boolean isInMain() {
        return inMain;
    }

    //获取某个 Value 对象对应的寄存器
    public Register getRegister(Value value) {
        if(registerPool == null) {
            return null;
        }
        return registerPool.get(value);
    }

    //获取当前已分配的寄存器Registers
    public ArrayList<Register> getRegisters() {
        ArrayList<Register> registers = new ArrayList<>();
        for (Register register : Register.values()) {
            if (registerPool.containsValue(register)) {
                registers.add(register);
            }
        }
        return registers;
    }


    //获取某个 Value 对象对应的栈偏移量
    public Integer getStackOffset(Value value) {
        return stackOffset.get(value);
    }

    // 减少当前栈偏移量
    public void decreaseStackOffset(int size) {
        currentStackOffset -= size;
        assert currentStackOffset >= 0;
    }
    // 增加当前栈偏移量
    public void increaseStackOffset(int size) {
        currentStackOffset += size;
    }

    //生成常量字符串的汇编代码
    public void buildConstStr(ConstStr constStr) {
        String label = constStr.getRealName();
        String ascii = constStr.getValue();
        AsciiAsm asm = new AsciiAsm(label, ascii);
        data.add(asm);
    }
    //生成全局变量的汇编指令
    public void buildGlobalVar(GlobalVar globalVar) {
        String name = globalVar.getRealName();
        LLVMType type = globalVar.getType();
        if (type.isInt32()) {
            handleInt32GlobalVar(globalVar, name);
        } else if (type.isInt8()) {
            handleInt8GlobalVar(globalVar, name);
        }
    }

    private void handleInt32GlobalVar(GlobalVar globalVar, String name) {
        if (globalVar.isZeroInitial()) {
            data.add(globalVar.getLen() > 0
                    ? new WordAsm(name, Collections.nCopies(globalVar.getLen(), 0))
                    : new WordAsm(name, Collections.singletonList(0)));
        } else {
            data.add(globalVar.getLen() > 0
                    ? new WordAsm(name, globalVar.getInitial())
                    : new WordAsm(name, globalVar.getInitial()));
        }
    }

    private void handleInt8GlobalVar(GlobalVar globalVar, String name) {
        if (globalVar.isZeroInitial()) {
            data.add(globalVar.getLen() > 0
                    ? new ByteAsm(name, globalVar.getLen())
                    : new ByteAsm(name, 0));
        } else {
            data.add(globalVar.getLen() > 0
                    ? new ByteAsm(name, globalVar.getInitial())
                    : new ByteAsm(name, globalVar.getInitial()));
        }
    }



    public void buildFunction(Function function) {
        text.add(new LableAsm(function.getRealName() + ":", false));
        initFunction(function);
        allocateFunctionParameters(function);
        allocateStackForInstructions(function);
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            buildBasicBlock(basicBlock);
        }
    }

    public void initFunction(Function function) {
        setCurrentFunction(function);
        setCurrentStackOffset(0);
        registerPool = function.getRegisterPool();
        stackOffset = new HashMap<>();
    }

    private void allocateFunctionParameters(Function function) {
        List<Param> params = function.getParams();
        for (int i = 0; i < params.size(); i++) {
            decreaseStackOffset(4);
            Param param = params.get(i);
            stackOffset.put(param, currentStackOffset);
            if (i < 3) {
                registerPool.put(param, Register.getRegister(Register.A0.ordinal() + i + 1));
            }
        }
    }

    private void allocateStackForInstructions(Function function) {
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction instruction : basicBlock.getInstrs()) {
                if (instruction.hasLVal() && !stackOffset.containsKey(instruction) && !registerPool.containsKey(instruction)) {
                    decreaseStackOffset(4);
                    stackOffset.put(instruction, currentStackOffset);
                }
            }
        }
    }

    //生成基本块的汇编代码
    public void buildBasicBlock(BasicBlock basicBlock) {
        text.add(new LableAsm("\t" + basicBlock.getParentFunc().getRealName() + "_" + basicBlock.getName(), true));
        for (Instruction instruction : basicBlock.getInstrs()) {
            buildInstruction(instruction);
        }
    }

    //生成指令的汇编代码
    public void buildInstruction(Instruction instruction) {
        text.add(new Comment(instruction));
        switch (instruction.getInstrType()) {
            case ALU -> buildAlu((Alu) instruction);
            case ALLOCA -> buildAlloca((Alloca) instruction);
            case BRANCH -> buildBranch((Branch) instruction);
            case CALL -> buildCall((Call) instruction);
            case GETPTR -> buildGetPtr((GetPtr) instruction);
            case ICMP -> buildIcmp((Icmp) instruction);
            case LOAD -> buildLoad((Load) instruction);
            case RETURN -> buildRet((Ret) instruction);
            case STORE -> buildStore((Store) instruction);
            case ZEXT -> buildZext((Zext) instruction);
            case TRUNC -> buildTrunc((Trunc) instruction);
            case GETINT -> buildGetInt((Getint) instruction);
            case PUTSTR -> buildPutStr((Putstr) instruction);
            case PUTINT -> buildPutInt((Putint) instruction);
            case GETCHAR -> buildGetChar((Getchar) instruction);
            case PUTCH -> buildPutCh((Putch) instruction);
            default -> {}
        }
    }

    private Register loadImmediate(int value, Register defaultRegister) {
        if (immCache.containsKey(value)) {
            return immCache.get(value);
        }
        text.add(new Li(defaultRegister, value));
        immCache.put(value, defaultRegister);
        return defaultRegister;
    }

    private Register loadOperand(Value operand, Register defaultRegister) {
        if (operand instanceof Constant) {
            return loadImmediate(((Constant) operand).getValue(), defaultRegister);
        } else if (registerPool.containsKey(operand)) {
            return getRegister(operand);
        } else {
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(operand), Register.getRegister(Register.SP.ordinal()), defaultRegister));
            return defaultRegister;
        }
    }

    public void buildAlu(Alu alu) {
        // 获取操作数和操作符
        Value op1 = alu.getOperands().get(0);
        Value op2 = alu.getOperands().get(1);
        Alu.OP op = alu.getOp();

        // 确定目标寄存器
        Register rd = getRegister(alu) != null ? getRegister(alu) : Register.getRegister(Register.K0.ordinal());

        // 加载操作数
        Register rs = loadOperand(op1, Register.getRegister(Register.K0.ordinal()));
        Register rt = loadOperand(op2, Register.getRegister(Register.K1.ordinal()));

        // 分支处理
        switch (determineOperandType(op1, op2)) {
            case "TWO_CONSTANTS" -> handleTwoConstants(op, rd, rs, rt, (Constant) op1, (Constant) op2);
            case "ONE_CONSTANT" -> handleOneConstant(op, rd, rs, rt, op1, op2);
            case "NO_CONSTANTS" -> handleNoConstants(op, rd, rs, rt);
        }

        // 如果目标寄存器是 Register.K0，则将结果存回内存
        if (rd == Register.getRegister(Register.K0.ordinal())) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(alu), Register.getRegister(Register.SP.ordinal()), rd));
        }
    }
    private String determineOperandType(Value op1, Value op2) {
        if (op1 instanceof Constant && op2 instanceof Constant) {
            return "TWO_CONSTANTS";
        } else if (op1 instanceof Constant || op2 instanceof Constant) {
            return "ONE_CONSTANT";
        } else {
            return "NO_CONSTANTS";
        }
    }
    private void handleTwoConstants(Alu.OP op, Register rd, Register rs, Register rt, Constant c1, Constant c2) {
        text.add(new Li(rs, c1.getValue()));
        text.add(new Li(rt, c2.getValue()));

        if (op == Alu.OP.ADD) {
            text.add(new AluAsm(AluAsm.AluOp.addu, rd, rs, rt));
        } else if (op == Alu.OP.SUB) {
            text.add(new AluAsm(AluAsm.AluOp.subu, rd, rs, rt));
        } else if (op == Alu.OP.MUL) {
            text.add(new AluAsm(AluAsm.AluOp.mul, rd, rs, rt));
        } else if (op == Alu.OP.SDIV) {
            text.add(new AluAsm(AluAsm.AluOp.div, rs, rt));
            text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
        } else if (op == Alu.OP.SREM) {
            text.add(new AluAsm(AluAsm.AluOp.div, rs, rt));
            text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
        }
    }
    private void handleOneConstant(Alu.OP op, Register rd, Register rs, Register rt, Value op1, Value op2) {
        Constant constant = (op1 instanceof Constant) ? (Constant) op1 : (Constant) op2;
        Value nonConstant = (op1 instanceof Constant) ? op2 : op1;

        Register constantRegister = loadOperand(constant, rt);
        Register nonConstantRegister = loadOperand(nonConstant, rs);

        if (op == Alu.OP.ADD || op == Alu.OP.SUB) {
            int value = (op == Alu.OP.SUB && op1 instanceof Constant) ? -constant.getValue() : constant.getValue();
            text.add(new AluAsm(AluAsm.AluOp.addiu, rd, nonConstantRegister, value));
        } else if (op == Alu.OP.MUL) {
            text.add(new AluAsm(AluAsm.AluOp.mul, rd, constantRegister, nonConstantRegister));
        } else if (op == Alu.OP.SDIV) {
            text.add(new AluAsm(AluAsm.AluOp.div, nonConstantRegister, constantRegister));
            text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
        } else if (op == Alu.OP.SREM) {
            text.add(new AluAsm(AluAsm.AluOp.div, nonConstantRegister, constantRegister));
            text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
        }
    }

    private void handleNoConstants(Alu.OP op, Register rd, Register rs, Register rt) {
        if (op == Alu.OP.ADD) {
            text.add(new AluAsm(AluAsm.AluOp.addu, rd, rs, rt));
        } else if (op == Alu.OP.SUB) {
            text.add(new AluAsm(AluAsm.AluOp.subu, rd, rs, rt));
        } else if (op == Alu.OP.MUL) {
            text.add(new AluAsm(AluAsm.AluOp.mul, rd, rs, rt));
        } else if (op == Alu.OP.SDIV) {
            text.add(new AluAsm(AluAsm.AluOp.div, rs, rt));
            text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
        } else if (op == Alu.OP.SREM) {
            text.add(new AluAsm(AluAsm.AluOp.div, rs, rt));
            text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
        }
    }

    //生成 ALLOCA 指令的汇编代码
    public void buildAlloca(Alloca alloca) {
        // 获取指向的类型并更新栈偏移量
        updateStackOffset(alloca);

        // 判断 Alloca 是否在寄存器池中
        if (registerPool.containsKey(alloca)) {
            handleRegisterAlloca(alloca);
        } else {
            handleMemoryAlloca(alloca);
        }
    }

    // 更新栈偏移量
    private void updateStackOffset(Alloca alloca) {
        LLVMType type = alloca.getPointedType();
        if (type.isArray()) {
            // 如果是数组类型，根据长度计算所需栈空间
            ArrayType arrayType = (ArrayType) type;
            decreaseStackOffset(arrayType.getLength() * 4);
        } else {
            // 如果不是数组类型，默认分配 4 字节
            decreaseStackOffset(4);
        }
    }

    // 处理在寄存器池中的 Alloca
    private void handleRegisterAlloca(Alloca alloca) {
        // 获取目标寄存器
        Register rd = getRegister(alloca);

        // 生成指令：sp + offset -> rd
        text.add(new AluAsm(AluAsm.AluOp.addiu, rd, Register.getRegister(Register.SP.ordinal()), getCurrentStackOffset()));
    }

    // 处理不在寄存器池中的 Alloca
    private void handleMemoryAlloca(Alloca alloca) {
        // 使用临时寄存器 $k0 计算偏移量
        Register tempReg = Register.getRegister(Register.K0.ordinal());

        // 生成指令：sp + offset -> $k0
        text.add(new AluAsm(AluAsm.AluOp.addiu, tempReg, Register.getRegister(Register.SP.ordinal()), getCurrentStackOffset()));

        // 将计算结果存储到栈中
        text.add(new Mem(Mem.MemOp.sw, getStackOffset(alloca), Register.getRegister(Register.SP.ordinal()), tempReg));
    }

    //生成 BRANCH 指令的汇编代码
    public void buildBranch(Branch branch) {
        // 无条件分支
        if (!branch.isConditional()) {
            text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName() + "_" + branch.getTargetBlock().getName()));
            return;
        }

        // 获取条件分支的第一个操作数
        Icmp icmp = (Icmp) branch.getOperands().get(0);

        // 检查是否是控制流指令
        if (!icmp.isControlFlow()) {
            handleControlFlowBranch(branch, icmp);
            return;
        }

        // 根据 Icmp 操作符，映射到 MIPS 的分支指令
        BranchAsm.BranchOp branchOp = mapIcmpOpToBranchOp(icmp.getOp());

        // 获取操作数
        Value op1 = icmp.getOperands().get(0);
        Value op2 = icmp.getOperands().get(1);

        // 情况 1：两个操作数都是常量
        if (op1 instanceof Constant && op2 instanceof Constant) {
            int value1 = ((Constant) op1).getValue();
            int value2 = ((Constant) op2).getValue();

            // 预计算结果决定直接跳转
            boolean conditionMet = evaluateIcmpCondition(branchOp, value1, value2);
            if (conditionMet) {
                text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
            } else {
                text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName() + "_" + branch.getElseBlock().getName()));
            }
            return;
        }

        // 情况 2：一个操作数是常量
        if (op1 instanceof Constant || op2 instanceof Constant) {
            int constantValue = (op1 instanceof Constant) ? ((Constant) op1).getValue() : ((Constant) op2).getValue();
            Value nonConstantValue = (op1 instanceof Constant) ? op2 : op1;

            // 加载非常量操作数
            Register reg = loadOperand(nonConstantValue, Register.getRegister(Register.K0.ordinal()));

            // 使用常量与寄存器进行比较
            text.add(new BranchAsm(branchOp, reg, constantValue, currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
            text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName() + "_" + branch.getElseBlock().getName()));
            return;
        }

        // 情况 3：两个操作数都不是常量
        Register rs = loadOperand(op1, Register.getRegister(Register.K0.ordinal()));
        Register rt = loadOperand(op2, Register.getRegister(Register.K1.ordinal()));

        // 使用两个寄存器进行比较
        text.add(new BranchAsm(branchOp, rs, rt, currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
        text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName() + "_" + branch.getElseBlock().getName()));
    }
    private boolean evaluateIcmpCondition(BranchAsm.BranchOp branchOp, int value1, int value2) {
        return switch (branchOp) {
            case beq -> value1 == value2;
            case bne -> value1 != value2;
            case blt -> value1 < value2;
            case ble -> value1 <= value2;
            case bgt -> value1 > value2;
            case bge -> value1 >= value2;
            default -> throw new IllegalArgumentException("Unsupported Branch operation: " + branchOp);
        };
    }


    // 辅助方法：处理非控制流分支
    private void handleControlFlowBranch(Branch branch, Icmp icmp) {
        Register rd = getRegister(icmp);
        if (rd != null) {
            text.add(new BranchAsm(BranchAsm.BranchOp.beq, rd, Register.getRegister(Register.K0.ordinal()), currentFunction.getRealName() + "_" + branch.getElseBlock().getName()));
        } else {
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(icmp), Register.getRegister(Register.SP.ordinal()), Register.getRegister(Register.K0.ordinal())));
            text.add(new BranchAsm(BranchAsm.BranchOp.beq, Register.getRegister(Register.K0.ordinal()), 1, currentFunction.getRealName() + "_" + branch.getElseBlock().getName()));
        }
        text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName() + "_" + branch.getElseBlock().getName()));
    }

    // 辅助方法：将 Icmp 操作符映射到分支操作符
    private BranchAsm.BranchOp mapIcmpOpToBranchOp(Icmp.OP op) {
        return switch (op) {
            case EQ -> BranchAsm.BranchOp.beq;
            case NE -> BranchAsm.BranchOp.bne;
            case SLT -> BranchAsm.BranchOp.blt;
            case SLE -> BranchAsm.BranchOp.ble;
            case SGT -> BranchAsm.BranchOp.bgt;
            case SGE -> BranchAsm.BranchOp.bge;
        };
    }

//    // 生成 CALL 指令的汇编代码
//    public void buildCall(Call call) {
//        // 获取调用的函数
//        Function function = (Function) call.getOperands().get(0);
//        List<Value> params = call.getOperands().subList(1, call.getOperands().size());
//
//        // 1. 保存当前寄存器状态
//        List<Register> savedRegisters = saveRegisters();
//
//        // 2. 处理参数
//        handleCallParameters(params);
//
//        // 3. 调用函数
//        text.add(new JumpAsm(JumpAsm.JumpOp.jal, function.getRealName()));
//
//        // 4. 恢复寄存器状态
//        restoreRegisters(savedRegisters);
//
//        // 5. 处理返回值
//        handleReturnValue(call, function.getReturnType());
//    }
//
//    private List<Register> saveRegisters() {
//        List<Register> savedRegisters = getRegisters(); // 获取已分配的寄存器
//        for (Register reg : savedRegisters) {
//            int offset = allocateStackSlot();
//            text.add(new Mem(Mem.MemOp.sw, offset, Register.SP, reg));
//        }
//        // 保存返回地址（$ra）
//        int raOffset = allocateStackSlot();
//        text.add(new Mem(Mem.MemOp.sw, raOffset, Register.SP, Register.RA));
//        return savedRegisters;
//    }
//    private void handleCallParameters(List<Value> params) {
//        for (int i = 0; i < params.size(); i++) {
//            Value param = params.get(i);
//            if (i < 4) {
//                // 前四个参数放在 $a0-$a3
//                Register paramReg = Register.getRegister(Register.A0.ordinal() + i);
//                text.add(loadValueIntoRegister(param, paramReg));
//            } else {
//                // 超过四个参数放在栈上
//                int paramOffset = allocateStackSlot();
//                Register tempReg = Register.getRegister(Register.K0.ordinal()); // 临时寄存器
//                text.add(loadValueIntoRegister(param, tempReg));
//                text.add(new Mem(Mem.MemOp.sw, paramOffset, Register.SP, tempReg));
//            }
//        }
//    }
//
//    private AsmInstruction loadValueIntoRegister(Value value, Register targetRegister) {
//        if (value instanceof Constant) {
//            return new Li(targetRegister, ((Constant) value).getValue());
//        } else if (registerPool.containsKey(value)) {
//            return new MoveAsm(targetRegister, getRegister(value));
//        } else {
//            int offset = getStackOffset(value);
//            return new Mem(Mem.MemOp.lw, offset, Register.SP, targetRegister);
//        }
//    }
//
//    private void restoreRegisters(List<Register> savedRegisters) {
//        for (int i = savedRegisters.size() - 1; i >= 0; i--) {
//            Register reg = savedRegisters.get(i);
//            int offset = deallocateStackSlot();
//            text.add(new Mem(Mem.MemOp.lw, offset, Register.SP, reg));
//        }
//        // 恢复返回地址（$ra）
//        int raOffset = deallocateStackSlot();
//        text.add(new Mem(Mem.MemOp.lw, raOffset, Register.SP, Register.RA));
//    }
//
//    private void handleReturnValue(Call call, LLVMType returnType) {
//        if (returnType.isVoid()) {
//            return; // 无返回值
//        }
//        Register destReg = getRegister(call);
//        if (destReg != null) {
//            // 返回值放入目标寄存器
//            text.add(new AluAsm(AluAsm.AluOp.addiu, destReg, Register.V0, 0));
//        } else {
//            // 返回值存储到栈中
//            int retOffset = getStackOffset(call);
//            text.add(new Mem(Mem.MemOp.sw, retOffset, Register.SP, Register.V0));
//        }
//    }
//
//    private int allocateStackSlot() {
//        decreaseStackOffset(4); // 每次分配 4 字节
//        return getCurrentStackOffset();
//    }
//
//    private int deallocateStackSlot() {
//        int offset = getCurrentStackOffset();
//        increaseStackOffset(4); // 每次释放 4 字节
//        return offset;
//    }

    // 生成 CALL 指令的汇编代码
    public void buildCall(Call call) {
        // 获取调用的函数
        Function function = (Function) call.getOperands().get(0);
        // 获取参数列表
        List<Value> params = call.getOperands().subList(1, call.getOperands().size());

        // 1. 寄存器分配
        // 获取寄存器池中的所有寄存器 Hashset
        ArrayList<Register> registers = getRegisters();

        // 2. 初始化保存和恢复指令列表
        ArrayList<Mem> loadAsms = new ArrayList<>();   // 加载指令列表
        ArrayList<Mem> storeAsms = new ArrayList<>();  // 存储指令列表

        // 3. 确保特定的参数寄存器（a1, a2, a3）被保存
        for (int i = 0; i < 3; i++) {
            Register paramReg = Register.getRegister(Register.A0.ordinal() + i + 1);
            if (!registers.contains(paramReg)) {
                registers.add(paramReg);
            }
        }

        // 4. 为保存的寄存器生成存储指令
        for (int i = 0; i < registers.size(); i++) {
            Register reg = registers.get(i);
            // 计算寄存器在栈中的存储位置
            int offset = getCurrentStackOffset() - (i + 1) * 4;
            // 生成 store word (sw) 指令，将寄存器的值存储到栈中
            Mem storeAsm = new Mem(Mem.MemOp.sw, offset, Register.SP, reg);
            text.add(storeAsm);
            storeAsms.add(storeAsm);
        }
        // 保存返回地址（ra）到栈中
        int raOffset = getCurrentStackOffset() - (registers.size() + 1) * 4;
        Mem storeAsm = new Mem(Mem.MemOp.sw, raOffset, Register.SP, Register.RA);
        text.add(storeAsm);
        storeAsms.add(storeAsm);

        // 5. 将参数存入寄存器或栈
        for (int i = 0; i < params.size(); i++) {
            Value param = params.get(i);
            if (i < 3) {
                // 前三个参数通过寄存器 a1, a2, a3 传递
                Register paramReg = Register.getRegister(Register.A0.ordinal() + i + 1);
                if (param instanceof Constant) {
                    // 如果参数是常量，使用 li 指令加载立即数
                    text.add(new Li(paramReg, ((Constant) param).getValue()));
                } else if (registerPool.containsKey(param)) {
                    Register sourceReg = registerPool.get(param);
                    if (param instanceof Param) {
                        // 如果参数是 Param 类型，从栈中加载到参数寄存器
                        int paramOffset = getStackOffset(param);
                        // 如果参数在栈中的偏移量为 0，说明参数在栈中的位置未分配
                        if (paramOffset == 0) {
                            paramOffset = getCurrentStackOffset()  - (registers.indexOf(sourceReg) + 1) * 4;
                        }
                        text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, paramReg));
                    } else {
                        // 否则，移动寄存器的值到参数寄存器
                        text.add(new MoveAsm(paramReg, sourceReg));
                    }
                } else {
                    // 如果参数不在寄存器中，从栈中加载到参数寄存器
                    int paramOffset = getStackOffset(param);
                    text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, paramReg));
                }
            } else {
                // 超过三个参数，通过栈传递
                Register tempReg = Register.getRegister(Register.K0.ordinal()); // 临时寄存器
                if (param instanceof Constant) {
                    // 如果参数是常量，使用 li 指令加载立即数到临时寄存器
                    text.add(new Li(tempReg, ((Constant) param).getValue()));
                } else if (registerPool.containsKey(param)) {
                    Register sourceReg = registerPool.get(param);
                    if (param instanceof Param) {
                        // 如果参数是 Param 类型，从栈中加载到临时寄存器
                        int paramOffset = getStackOffset(param);
                        // 如果参数在栈中的偏移量为 0，说明参数在栈中的位置未分配
                        if (paramOffset == 0) {
                            paramOffset = getCurrentStackOffset()  - (registers.indexOf(sourceReg) + 1) * 4;
                        }
                        text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, tempReg));
                    } else {
                        // 否则，将源寄存器的值复制到临时寄存器
                        text.add(new MoveAsm(tempReg, sourceReg));
                    }
                } else {
                    // 如果参数不在寄存器中，从栈中加载到临时寄存器
                    int paramOffset = getStackOffset(param);
                    text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, tempReg));
                }
                // 将参数存储到栈中的相应位置
                int stackOffset = getCurrentStackOffset() - (registers.size() + 2 + i) * 4;
                text.add(new Mem(Mem.MemOp.sw, stackOffset, Register.SP, tempReg));
            }
        }

        // 6. 调整栈指针，为保存的寄存器和返回地址分配空间
        int totalStackOffset = 4 * registers.size() + 4 - getCurrentStackOffset(); // 计算总的栈偏移量
        text.add(new AluAsm(AluAsm.AluOp.addiu, Register.SP, Register.SP, -totalStackOffset));

        // 7. 生成跳转并链接（jal）指令调用函数
        JumpAsm jal =  new JumpAsm(JumpAsm.JumpOp.jal, function.getRealName());
        text.add(jal);
        // 8. 恢复返回地址（ra）从栈中
        text.add(new Mem(Mem.MemOp.lw, raOffset, Register.SP, Register.RA));

        // 9. 调整栈指针，释放分配的空间
        text.add(new AluAsm(AluAsm.AluOp.addiu, Register.SP, Register.SP, totalStackOffset));

        // 10. 恢复保存的寄存器
        for (int i = 0; i < registers.size(); i++) {
            Register reg = registers.get(i);
            int offset = getCurrentStackOffset() - (i + 1) * 4;
            // 生成 load word (lw) 指令，将栈中的值加载到寄存器
            Mem loadAsm = new Mem(Mem.MemOp.lw, offset, Register.SP, reg);
            text.add(loadAsm);
            loadAsms.add(loadAsm);
        }
        // 设置 jal 指令的加载和存储指令列表
        jal.setLoadAsms(loadAsms);
        jal.setStoreAsms(storeAsms);
        // 11. 处理函数返回值
        if (function.getReturnType() != LLVMType.Void) {
            Register destReg = registerPool.get(call); // 获取存储返回值的目标寄存器
            if (destReg != null) {
                // 如果目标寄存器存在，将返回值从 v0 移动到目标寄存器
                text.add(new AluAsm(AluAsm.AluOp.addiu, destReg, Register.V0, 0));
            } else {
                // 否则，将返回值存储到栈中的相应位置
                int retOffset = getStackOffset(call);
                text.add(new Mem(Mem.MemOp.sw, retOffset, Register.SP, Register.V0));
            }
        }
    }
    // 生成 GETPTR 指令的汇编代码
    public void buildGetPtr(GetPtr getPtr) {
        // 获取指针的基地址
        Value base = getPtr.getOperands().get(0);
        // 获取指针的偏移量
        Value offset = getPtr.getOperands().get(1);
        // 获取目标寄存器
        Register rd = Register.getRegister(Register.K0.ordinal());
        // 处理基地址
        Register baseReg = Register.getRegister(Register.K0.ordinal());
        if (base instanceof GlobalVar) {
            // 如果基地址是全局变量，加载其地址到 k0 寄存器
            text.add(new La(baseReg, base.getRealName()));
        } else if (registerPool.containsKey(base)) {
            // 如果基地址已被分配到寄存器，直接使用该寄存器
            baseReg = getRegister(base);
        } else {
            // 否则，从栈中加载基地址的值到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(base), Register.SP, baseReg));
        }
        // 处理偏移量
        Register offsetReg = Register.getRegister(Register.K1.ordinal());
        if (offset instanceof Constant) {
            // 常量偏移量
            int value = ((Constant) offset).getValue() * 4;
            //结果存储在寄存器中：
            if (registerPool.containsKey(getPtr)) {
                //如果目标寄存器已被分配到寄存器，直接使用该寄存器
                rd = getRegister(getPtr);
                //使用加立即数指令将基地址寄存器与偏移量乘以元素大小相加，结果存储到目标寄存器中。
                text.add(new AluAsm(AluAsm.AluOp.addiu, rd, baseReg, value));
            } else {
                //使用加立即数指令将基地址寄存器与偏移量乘以元素大小相加，结果存储到临时结果寄存器中。
                text.add(new AluAsm(AluAsm.AluOp.addiu, offsetReg, baseReg, value));
                //使用存储字指令将临时结果寄存器的值存储到栈中指定的位置。
                text.add(new Mem(Mem.MemOp.sw, getStackOffset(getPtr), Register.SP, offsetReg));
            }
        } else {
            // 变量偏移量
            if (registerPool.containsKey(offset)) {
                // 如果偏移量已被分配到寄存器，直接使用该寄存器
                offsetReg = getRegister(offset);
            } else {
                // 否则，从栈中加载偏移量的值
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(offset), Register.SP, offsetReg));
            }
            // 将偏移量左移2位（相当于乘以4），然后将基地址与偏移量相加
            text.add(new AluAsm(AluAsm.AluOp.sll, offsetReg, offsetReg, 2));
            if (registerPool.containsKey(getPtr)) {
                // 如果目标寄存器已被分配到寄存器，直接使用该寄存器
                rd = getRegister(getPtr);
                //使用无符号加法指令将基地址寄存器与中间寄存器中的偏移量相加，结果存储到目标寄存器中。
                text.add(new AluAsm(AluAsm.AluOp.addu, rd, baseReg, offsetReg));
            } else {
                //使用无符号加法指令将基地址寄存器与中间寄存器中的偏移量相加，结果存储到临时结果寄存器中。
                text.add(new AluAsm(AluAsm.AluOp.addu, offsetReg, baseReg, offsetReg));
                //使用存储字指令将临时结果寄存器的值存储到栈中指定的位置。
                text.add(new Mem(Mem.MemOp.sw, getStackOffset(getPtr), Register.SP, offsetReg));
            }
        }
    }

    // 生成 ICMP 指令的汇编代码
    public void buildIcmp(Icmp icmp) {
        if (icmp.isControlFlow()) {
            return;
        }
        // 获取操作数和操作符
        Value op1 = icmp.getOperands().get(0);
        Value op2 = icmp.getOperands().get(1);
        Icmp.OP op = icmp.getOp();
        // 确定常量操作数的数量
        int constNum = 0;
        if (op1 instanceof Constant) {
            constNum++;
        }
        if (op2 instanceof Constant) {
            constNum++;
        }
        // 确定目标寄存器
        Register rd = getRegister(icmp) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(icmp);
        // 初始化用于存储操作数的寄存器
        Register rs = Register.getRegister(Register.K0.ordinal());
        Register rt = Register.getRegister(Register.K1.ordinal());
        // 将 LLVM 的比较操作映射到汇编层面的比较操作
        CmpAsm.CmpOp cmpOp = null;
        switch (op) {
            case EQ:
                cmpOp = CmpAsm.CmpOp.seq;
                break;
            case NE:
                cmpOp = CmpAsm.CmpOp.sne;
                break;
            case SLT:
                cmpOp = CmpAsm.CmpOp.slt;
                break;
            case SLE:
                cmpOp = CmpAsm.CmpOp.sle;
                break;
            case SGT:
                cmpOp = CmpAsm.CmpOp.sgt;
                break;
            case SGE:
                cmpOp = CmpAsm.CmpOp.sge;
                break;
            default:
                break;
        }
        // 处理第一个操作数
        if (op1 instanceof Constant) {
            // 如果第一个操作数是常量，使用 li 指令加载立即数
            text.add(new Li(rs, ((Constant) op1).getValue()));
        } else if (registerPool.containsKey(op1)) {
            // 如果第一个操作数已被分配到寄存器，直接使用该寄存器
            rs = getRegister(op1);
        } else {
            // 否则，从栈中加载第一个操作数的值到 rs 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(op1), Register.SP, rs));
        }
        // 处理第二个操作数
        if (op2 instanceof Constant) {
            // 如果第二个操作数是常量，使用 li 指令加载立即数
            text.add(new Li(rt, ((Constant) op2).getValue()));
        } else if (registerPool.containsKey(op2)) {
            // 如果第二个操作数已被分配到寄存器，直接使用该寄存器
            rt = getRegister(op2);
        } else {
            // 否则，从栈中加载第二个操作数的值到 rt 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(op2), Register.SP, rt));
        }
        // 生成比较指令
        // 如果 icmp 指令的结果已被分配到某个寄存器，生成比较指令并将结果存储到目标寄存器中
        if (registerPool.containsKey(icmp)) {
            text.add(new CmpAsm(cmpOp, rs, rt, getRegister(icmp)));
        } else {
            // 否则，生成比较指令将结果存储到临时寄存器（k0）中
            text.add(new CmpAsm(cmpOp, rs, rt, Register.K0));
            // 然后，将临时寄存器的值存储到栈中对应的位置
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(icmp), Register.SP, rd));
        }
    }


    // 生成 LOAD 指令的汇编代码
    public void buildLoad(Load load) {
        // 获取指针
        Value ptr = load.getOperands().get(0);
        // 获取目标寄存器
        Register rd = getRegister(load) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(load);
        // 处理指针
        Register ptrReg = Register.getRegister(Register.K0.ordinal());
        if (ptr instanceof GlobalVar) {
            // 如果指针是全局变量，加载其地址到 k0 寄存器
            text.add(new La(ptrReg, ptr.getRealName()));
        } else if (registerPool.containsKey(ptr)) {
            // 如果指针已被分配到寄存器，直接使用该寄存器
            ptrReg = getRegister(ptr);
        } else {
            // 否则，从栈中加载指针的值到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(ptr), Register.SP, ptrReg));
        }
        // 生成 load 指令，将指针指向的值加载到目标寄存器中
        text.add(new Mem(Mem.MemOp.lw, 0, ptrReg, rd));
        // 如果目标寄存器是 Register.K0，则将结果存回内存
        if (rd == Register.getRegister(Register.K0.ordinal())) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(load), Register.SP, rd));
        }
    }

    // 生成 STORE 指令的汇编代码
    public void buildStore(Store store) {
        // 获取指针和值
        Value ptr = store.getTo();
        Value val = store.getFrom();
        // 处理指针
        Register ptrReg = Register.getRegister(Register.K0.ordinal());
        if (ptr instanceof GlobalVar) {
            // 如果指针是全局变量，加载其地址到 k0 寄存器
            text.add(new La(ptrReg, ptr.getRealName()));
        } else if (registerPool.containsKey(ptr)) {
            // 如果指针已被分配到寄存器，直接使用该寄存器
            ptrReg = getRegister(ptr);
        } else {
            // 否则，从栈中加载指针的值到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(ptr), Register.SP, ptrReg));
        }
        // 处理值
        Register valReg = Register.getRegister(Register.K1.ordinal());
        if (val instanceof Constant || val instanceof Undef) {
            // 如果值是常量，使用 li 指令加载立即数
            text.add(new Li(valReg, ((Constant) val).getValue()));
        } else if (registerPool.containsKey(val)) {
            // 如果值已被分配到寄存器，直接使用该寄存器
            valReg = getRegister(val);
        } else {
            // 否则，从栈中加载值到 k1 寄存器
            Integer valOffset = getStackOffset(val);
            if (valOffset == null) {
                decreaseStackOffset(4);
                valOffset = getCurrentStackOffset();
                stackOffset.put(val, valOffset);
            }
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(val), Register.SP, valReg));
        }
        // 生成 store 指令，将值存储到指针指向的位置
        text.add(new Mem(Mem.MemOp.sw, 0, ptrReg, valReg));
    }

    // 生成 TRUNC 指令的汇编代码
    public void buildTrunc(Trunc trunc) {
        // 获取源值和目标寄存器
        Value src = trunc.getOperands().get(0);
        Register rd = getRegister(trunc) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(trunc);
        // 处理源值
        Register srcReg = Register.getRegister(Register.K0.ordinal());
        if (src instanceof Constant) {
            // 如果源值是常量，使用 li 指令加载立即数
            text.add(new Li(srcReg, ((Constant) src).getValue()));
        } else if (registerPool.containsKey(src)) {
            // 如果源值已被分配到寄存器，直接使用该寄存器
            srcReg = getRegister(src);
        } else {
            // 否则，从栈中加载源值到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(src), Register.SP, srcReg));
        }
        if(registerPool.containsKey(trunc)) {
            text.add(new AluAsm(AluAsm.AluOp.andi, rd, srcReg, 0xFF));
        } else {
            // 将结果存储到栈中
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(trunc), Register.SP, rd));
        }
    }

    // 生成 ZEXT 指令的汇编代码
    public void buildZext(Zext zext) {
        // 获取源值和目标寄存器
        Value src = zext.getOperands().get(0);
        Register rd = getRegister(zext) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(zext);
        // 处理源值
        Register srcReg = Register.getRegister(Register.K0.ordinal());
        if (src instanceof Constant) {
            // 如果源值是常量，使用 li 指令加载立即数
            text.add(new Li(srcReg, ((Constant) src).getValue()));
        } else if (registerPool.containsKey(src)) {
            // 如果源值已被分配到寄存器，直接使用该寄存器
            srcReg = getRegister(src);
        } else {
            // 否则，从栈中加载源值到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(src), Register.SP, srcReg));
        }
    }


    // 生成 RET 指令的汇编代码
    public void buildRet(Ret ret) {
        // 如果是主函数，生成 Li 指令加载立即数 10 到 v0 寄存器，然后生成 syscall 指令
        if (currentFunction.getRealName().equals("main")) {
            text.add(new Li(Register.V0, 10));
            text.add(new Syscall());
            return;
        }
        // 获取返回值
        Value val = ret.getOperands().get(0);
        // 如果返回值为空，直接生成 ret 指令
        if (val == null) {
            text.add(new JumpAsm(JumpAsm.JumpOp.jr, Register.RA));
            return;
        }
        // 处理返回值
        Register valReg = Register.getRegister(Register.K0.ordinal());
        if (val instanceof Constant) {
            // 如果返回值是常量，使用 li 指令加载立即数
            text.add(new Li(valReg, ((Constant) val).getValue()));
        } else if (registerPool.containsKey(val)) {
            // 如果返回值已被分配到寄存器，直接使用该寄存器
            valReg = getRegister(val);
            text.add(new AluAsm(AluAsm.AluOp.addiu, Register.V0, valReg, 0));
        } else {
            // 否则，从栈中加载返回值到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(val), Register.SP, Register.V0));
        }
        // 生成无条件跳转指令
        text.add(new JumpAsm(JumpAsm.JumpOp.jr, Register.RA));
    }

    // 生成 GetInt 指令的汇编代码
    public void buildGetInt(Getint getInt) {
        // 生成 li 指令加载立即数 5 到 v0 寄存器
        text.add(new Li(Register.V0, 5));
        // 生成 syscall 指令
        text.add(new Syscall());
        // 将返回值存储到目标寄存器
        Register rd = getRegister(getInt) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(getInt);
        text.add(new MoveAsm(rd, Register.V0));
        // 如果目标寄存器是 Register.K0，则将结果存回内存
        if (rd == Register.getRegister(Register.K0.ordinal())) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(getInt), Register.SP, rd));
        }
    }

    // 生成 PutInt 指令的汇编代码
    public void buildPutInt(Putint putInt) {
        // 获取参数
        Value val = putInt.getOperands().get(0);
        // 处理参数
        if (val instanceof Constant) {
            // 如果参数是常量，使用 li 指令加载立即数
            text.add(new Li(Register.A0, ((Constant) val).getValue()));
        } else if (registerPool.containsKey(val)) {
            // 如果参数已被分配到寄存器，直接使用该寄存器
            text.add(new AluAsm(AluAsm.AluOp.addiu, Register.A0, getRegister(val), 0));
        } else {
            // 否则，从栈中加载参数到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(val), Register.SP, Register.A0));
        }
        // 生成 li 指令加载立即数 1 到 v0 寄存器
        text.add(new Li(Register.V0, 1));
        // 生成 syscall 指令
        text.add(new Syscall());
    }

    // 生成 PutStr 指令的汇编代码
    public void buildPutStr(Putstr putStr) {
        // 生成 la 指令加载字符串地址到 a0 寄存器
        text.add(new La(Register.A0, putStr.getConstStr().getRealName()));
        // 生成 li 指令加载立即数 4 到 v0 寄存器
        text.add(new Li(Register.V0, 4));
        // 生成 syscall 指令
        text.add(new Syscall());
    }

    // 生成 GetChar 指令的汇编代码
    public void buildGetChar(Getchar getchar) {
        // 生成 li 指令加载立即数 12 到 v0 寄存器
        text.add(new Li(Register.V0, 12));
        // 生成 syscall 指令
        text.add(new Syscall());
        // 将返回值存储到目标寄存器
        Register rd = getRegister(getchar) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(getchar);
        text.add(new MoveAsm(rd, Register.V0));
        // 如果目标寄存器是 Register.K0，则将结果存回内存
        if (rd == Register.getRegister(Register.K0.ordinal())) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(getchar), Register.SP, rd));
        }
    }

    // 生成 PutCh 指令的汇编代码
    public void buildPutCh(Putch putCh) {
        // 获取参数
        Value val = putCh.getOperands().get(0);
        // 处理参数
        Register valReg = Register.getRegister(Register.K0.ordinal());
        if (val instanceof Constant) {
            // 如果参数是常量，使用 li 指令加载立即数
            text.add(new Li(valReg, ((Constant) val).getValue()));
        } else if (registerPool.containsKey(val)) {
            // 如果参数已被分配到寄存器，直接使用该寄存器
            valReg = getRegister(val);
        } else {
            // 否则，从栈中加载参数到 k0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(val), Register.SP, valReg));
        }
        // 生成 li 指令加载立即数 11 到 v0 寄存器
        text.add(new Li(Register.V0, 11));
        // 生成 syscall 指令
        text.add(new Syscall());
    }

    // 生成所有的汇编代码
    public void mipsBuilder(Module module) {
//        // 初始化寄存器池，调用RegisterAllocator类的allocateRegisters方法
//        RegisterAllocator allocator = new RegisterAllocator();
//        allocator.allocateRegisters(module);
        //初始化两个 ArrayList，分别用于存储数据段和文本段的汇编指令
        data = new ArrayList<>();
        text = new ArrayList<>();
        //遍历模块中的全局变量，生成数据段的汇编代码
        for (GlobalVar globalVar : module.getGlobalVars()) {
            buildGlobalVar(globalVar);
        }
        //遍历模块中的ConstStr
        for (ConstStr constStr : module.getConstStrs()) {
            buildConstStr(constStr);
        }
        //遍历模块中的函数，生成文本段的汇编代码
        for (Function function : module.getFunctions()) {
            // 优先处理main函数
            if (function.getRealName().equals("main")) {
                setInMain(true);
                buildFunction(function);
                setInMain(false);
            }
        }
        //遍历模块中的函数，生成文本段的汇编代码
        for (Function function : module.getFunctions()) {
            // 处理非 main 函数
            if (!function.getRealName().equals("main")) {
                buildFunction(function);
            }
        }
        //将数据段和文本段的汇编代码写入文件
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("mips.txt"));
            writer.println(".data");
            for (GlobalAsm dataAsm : data) {
                writer.println(dataAsm.toString());
            }
            writer.println(".text");
            for (AsmInstruction textAsm : text) {
                //如果不是LabelAsm类型，添加缩进
                if (!(textAsm instanceof LableAsm)) {
                    writer.print("\t\t");
                }
                writer.println(textAsm.toString());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}
