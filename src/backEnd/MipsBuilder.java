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
import midEnd.Optimizer;


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
        if (stackOffset.get(value) == null) {
            decreaseStackOffset(4);
            int valOffset = getCurrentStackOffset();
            stackOffset.put(value, valOffset);
        }
        return stackOffset.get(value);
    }

    // 减少当前栈偏移量
    public void decreaseStackOffset(int size) {
        currentStackOffset -= size;
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
//        if (type.isInt32()) {
//            handleInt32GlobalVar(globalVar, name);
//        } else if (type.isInt8()) {
//            handleInt8GlobalVar(globalVar, name);
//        }
        handleInt32GlobalVar(globalVar, name);
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
                }else if(instruction instanceof Move){
                    Move move = (Move) instruction;
                    if(!stackOffset.containsKey(move.getTo()) && !registerPool.containsKey(move.getTo())){
                        decreaseStackOffset(4);
                        stackOffset.put(move.getTo(), currentStackOffset);
                    }
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
            case ALU ->{
                if(Optimizer.getInstance().isBasicOptimize()) {
                    buildBasicAlu((Alu) instruction);
                }
                else {
                    buildAlu((Alu) instruction);
                }
            }
            case ALLOCA -> buildAlloca((Alloca) instruction);
            case BRANCH -> buildBranch((Branch) instruction);
            case CALL -> buildCall((Call) instruction);
            case GETPTR -> buildGetPtr((GetPtr) instruction);
            case ICMP -> buildIcmp((Icmp) instruction);
            case LOAD -> buildLoad((Load) instruction);
            case RETURN -> buildRet((Ret) instruction);
            case STORE -> buildStore((Store) instruction);
            case TRUNC -> buildTrunc((Trunc) instruction);
            case GETINT -> buildGetInt((Getint) instruction);
            case PUTSTR -> buildPutStr((Putstr) instruction);
            case PUTINT -> buildPutInt((Putint) instruction);
            case GETCHAR -> buildGetChar((Getchar) instruction);
            case PUTCH -> buildPutCh((Putch) instruction);
            case MOVE -> buildMove((Move) instruction);
            case ZEXT -> buildZext((Zext) instruction);
            default -> {}
        }
    }


    private Register loadOperand(Value operand, Register defaultRegister) {
        if (operand instanceof Constant) {
            text.add(new Li(defaultRegister, ((Constant) operand).getValue()));
            return defaultRegister;
        } else if (registerPool.containsKey(operand)) {
            return getRegister(operand);
        } else {
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(operand), Register.getRegister(Register.SP.ordinal()), defaultRegister));
            return defaultRegister;
        }
    }

        public void buildAlu(Alu aluInstr) {
            Value value1 = aluInstr.getOperands().get(0);
            Value value2 = aluInstr.getOperands().get(1);
            Alu.OP op = aluInstr.getOp();

            // 计算常量操作数的数量
            int constCount = 0;
            if (value1 instanceof Constant) constCount++;
            if (value2 instanceof Constant) constCount++;

            // 获取目标寄存器
            Register targetReg = registerPool.containsKey(aluInstr) ?
                    registerPool.get(aluInstr) : Register.K0;

            if (constCount == 2) {
                handleTwoConstants(aluInstr, value1, value2, op, targetReg);
            } else if (constCount == 1) {
                handleOneConstant(aluInstr, value1, value2, op, targetReg);
            } else {
                handleNoConstants(aluInstr, value1, value2, op, targetReg);
            }

            // 如果使用临时寄存器，需要存回栈中
            if (targetReg == Register.K0) {
                text.add(new Mem(Mem.MemOp.sw, stackOffset.get(aluInstr), Register.SP, Register.K0));
            }
        }

        private void handleTwoConstants(Alu aluInstr, Value value1, Value value2,
                                        Alu.OP op, Register targetReg) {
            text.add(new Li(Register.K0, ((Constant) value1).getValue()));

            if (op == Alu.OP.ADD || op == Alu.OP.SUB) {
                int num = op == Alu.OP.ADD ?
                        ((Constant) value2).getValue() :
                        -((Constant) value2).getValue();
                text.add(new AluAsm(AluAsm.AluOp.addiu, targetReg, Register.K0, num));
                return;
            }

            text.add(new Li(Register.K1, ((Constant) value2).getValue()));
            if (op == Alu.OP.MUL) {
                text.add(new AluAsm(AluAsm.AluOp.mul, targetReg, Register.K0, Register.K1));
            } else {
                text.add(new AluAsm(AluAsm.AluOp.div, Register.K0, Register.K1));
                text.add(new MoveFrom(op == Alu.OP.SREM ? MoveFrom.Type.MFHI : MoveFrom.Type.MFLO, targetReg));
            }
        }

        private void handleOneConstant(Alu aluInstr, Value value1, Value value2,
                                       Alu.OP op, Register targetReg) {
            if (value1 instanceof Constant) {
                handleConstantFirstOperand(value1, value2, op, targetReg);
            } else {
                handleConstantSecondOperand(value1, value2, op, targetReg);
            }
        }

        private void handleConstantFirstOperand(Value value1, Value value2,
                                                Alu.OP op, Register targetReg) {
            Register operand2 = loadValueToRegister(value2, Register.K0);
            int constant = ((Constant) value1).getValue();

            if (op == Alu.OP.ADD) {
                text.add(new AluAsm(AluAsm.AluOp.addiu, targetReg, operand2, constant));
            } else if (op == Alu.OP.MUL) {
                buildMulWithCons(operand2, constant, targetReg);
            } else {
                text.add(new Li(Register.K1, constant));
                if (op == Alu.OP.SUB) {
                    text.add(new AluAsm(AluAsm.AluOp.subu, targetReg, Register.K1, operand2));
                } else {
                    text.add(new AluAsm(AluAsm.AluOp.div, Register.K1, operand2));
                    text.add(new MoveFrom(op == Alu.OP.SDIV ? MoveFrom.Type.MFLO : MoveFrom.Type.MFHI, targetReg));
                }
            }
        }

        private void handleConstantSecondOperand(Value value1, Value value2,
                                                 Alu.OP op, Register targetReg) {
            Register operand1 = loadValueToRegister(value1, Register.K0);
            int constant = ((Constant) value2).getValue();

            if (op == Alu.OP.ADD || op == Alu.OP.SUB) {
                text.add(new AluAsm(AluAsm.AluOp.addiu, targetReg, operand1,
                        op == Alu.OP.ADD ? constant : -constant));
            } else if (op == Alu.OP.MUL) {
                buildMulWithCons(operand1, constant, targetReg);
            } else if (op == Alu.OP.SDIV) {
                buildDivWithCons(operand1, constant, targetReg);
            } else {
                text.add(new Li(Register.K1, constant));
                text.add(new AluAsm(AluAsm.AluOp.div, operand1, Register.K1));
                text.add(new MoveFrom(MoveFrom.Type.MFHI, targetReg));
            }
        }

        private void handleNoConstants(Alu aluInstr, Value value1, Value value2,
                                       Alu.OP op, Register targetReg) {
            Register operand1 = loadValueToRegister(value1, Register.K0);
            Register operand2 = loadValueToRegister(value2, Register.K1);

            switch (op) {
                case ADD -> text.add(new AluAsm(AluAsm.AluOp.addu, targetReg, operand1, operand2));
                case SUB -> text.add(new AluAsm(AluAsm.AluOp.subu, targetReg, operand1, operand2));
                case MUL -> text.add(new AluAsm(AluAsm.AluOp.mul, targetReg, operand1, operand2));
                case SDIV -> {
                    text.add(new AluAsm(AluAsm.AluOp.div, operand1, operand2));
                    text.add(new MoveFrom(MoveFrom.Type.MFLO, targetReg));
                }
                case SREM -> {
                    text.add(new AluAsm(AluAsm.AluOp.div, operand1, operand2));
                    text.add(new MoveFrom(MoveFrom.Type.MFHI, targetReg));
                }
            }
        }

        private Register loadValueToRegister(Value value, Register defaultReg) {
            if (registerPool.containsKey(value)) {
                return registerPool.get(value);
            }
            text.add(new Mem(Mem.MemOp.lw, stackOffset.get(value), Register.SP, defaultReg));
            return defaultReg;
        }

        public void buildMulWithCons(Register src, int cons, Register to) {
            if (cons < 0 || countSetBits(cons) > 2) {
                text.add(new Li(Register.V0, cons));
                text.add(new AluAsm(AluAsm.AluOp.mul, to, Register.V0, src));
                return;
            }

            ShiftInfo shifts = calculateShifts(cons);
            if (shifts.shiftCount == 1) {
                text.add(new AluAsm(AluAsm.AluOp.sll, to, src, shifts.shift1));
            } else {
                buildTwoShiftMultiplication(src, shifts, to);
            }
        }

        private void buildTwoShiftMultiplication(Register src, ShiftInfo shifts, Register to) {
            if (shifts.shift1 == 0) {
                text.add(new AluAsm(AluAsm.AluOp.sll, Register.V1, src, shifts.shift2));
                text.add(new AluAsm(AluAsm.AluOp.addu, to, src, Register.V1));
            } else {
                text.add(new AluAsm(AluAsm.AluOp.sll, Register.V0, src, shifts.shift1));
                text.add(new AluAsm(AluAsm.AluOp.sll, Register.V1, src, shifts.shift2));
                text.add(new AluAsm(AluAsm.AluOp.addu, to, Register.V0, Register.V1));
            }
        }

        private int countSetBits(int n) {
            int count = 0;
            while (n != 0) {
                count += n & 1;
                n >>>= 1;
            }
            return count;
        }

        private ShiftInfo calculateShifts(int n) {
            int shift1 = -1, shift2 = -1;
            int count = 0;
            int pos = 0;

            while (n != 0) {
                if ((n & 1) == 1) {
                    if (count == 0) shift1 = pos;
                    if (count == 1) shift2 = pos;
                    count++;
                }
                n >>>= 1;
                pos++;
            }

            return new ShiftInfo(count, shift1, shift2);
        }

        public void buildDivWithCons(Register src, int cons, Register to) {
            if (cons == 1) {
                if (to != src) {
                    text.add(new AluAsm(AluAsm.AluOp.addu, to, src, Register.ZERO));
                }
                return;
            }
            if (cons == -1) {
                text.add(new AluAsm(AluAsm.AluOp.subu, to, Register.ZERO, src));
                return;
            }

            int abs = Math.abs(cons);
            if ((abs & (abs - 1)) == 0) {
                handlePowerOfTwoDivision(src, abs, to);
            } else {
                handleGeneralDivision(src, cons, to);
            }

            if (cons < 0) {
                text.add(new AluAsm(AluAsm.AluOp.subu, to, Register.ZERO, to));
            }
        }

        private void handlePowerOfTwoDivision(Register src, int abs, Register to) {
            int l = Integer.numberOfTrailingZeros(abs);
            Register dividend = getDividend(src, abs);
            text.add(new AluAsm(AluAsm.AluOp.sra, to, dividend, l));
        }

        private Register getDividend(Register oldDividend, int abs) {
            int l = getSllCounts(abs);
            text.add(new AluAsm(AluAsm.AluOp.sra, Register.V0, oldDividend, 31));
            if (l > 0) {
                text.add(new AluAsm(AluAsm.AluOp.srl, Register.V0, Register.V0, 32 - l));
            }
            text.add(new AluAsm(AluAsm.AluOp.addu, Register.V1, oldDividend, Register.V0));
            return Register.V1;
        }

        public int getSllCounts(int temp) {
            return 32 - Integer.numberOfLeadingZeros(temp) - 1;
        }

        private void handleGeneralDivision(Register src, int cons, Register to) {
            long t = 32;
            int abs = Math.abs(cons);
            long nc = ((long) 1 << 31) - (((long) 1 << 31) % abs) - 1;

            while (((long) 1 << t) <= nc * (abs - ((long) 1 << t) % abs)) {
                t++;
            }

            long m = ((((long) 1 << t) + (long) abs - ((long) 1 << t) % abs) / (long) abs);
            int n = (int) ((m << 32) >>> 32);
            int shift = (int) (t - 32);

            text.add(new Li(Register.V0, n));
            if (m >= 0x80000000L) {
                text.add(new MoveTo(MoveTo.OP.hi, src));
                text.add(new AluAsm(AluAsm.AluOp.madd, Register.V1, src, Register.V0));
            } else {
                text.add(new AluAsm(AluAsm.AluOp.mult, src, Register.V0));
                text.add(new MoveFrom(MoveFrom.Type.MFHI, Register.V1));
            }
            text.add(new AluAsm(AluAsm.AluOp.sra, Register.V0, Register.V1, shift));
            text.add(new AluAsm(AluAsm.AluOp.srl, Register.A0, src, 31));
            text.add(new AluAsm(AluAsm.AluOp.addu, to, Register.V0, Register.A0));
        }

        private static class ShiftInfo {
            final int shiftCount;
            final int shift1;
            final int shift2;

            ShiftInfo(int shiftCount, int shift1, int shift2) {
                this.shiftCount = shiftCount;
                this.shift1 = shift1;
                this.shift2 = shift2;
            }
        }
    public void buildBasicAlu(Alu alu) {
        // 获取操作数和操作符
        Value op1 = alu.getOperands().get(0);
        Value op2 = alu.getOperands().get(1);
        Alu.OP op = alu.getOp();

        // 确定目标寄存器
        Register rd = getRegister(alu) != null ? getRegister(alu) : Register.getRegister(Register.K0.ordinal());

        // 分支处理
        switch (determineOperandType(op1, op2)) {
            case "TWO_CONSTANTS" -> handleTwoConstants(op, rd, (Constant) op1, (Constant) op2);
            case "ONE_CONSTANT" -> handleOneConstant(op, rd, op1, op2);
            case "NO_CONSTANTS" -> handleNoConstants(op, rd, op1, op2);
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
    private void handleTwoConstants(Alu.OP op, Register rd, Constant c1, Constant c2) {
        Register rs = loadOperand(c1, Register.getRegister(Register.K0.ordinal()));
        Register rt = loadOperand(c2, Register.getRegister(Register.K1.ordinal()));

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
    private void handleOneConstant(Alu.OP op, Register rd, Value op1, Value op2) {
        boolean isOp1Constant = op1 instanceof Constant;
        Constant constant = isOp1Constant ? (Constant) op1 : (Constant) op2;
        Value nonConstant = isOp1Constant ? op2 : op1;

        Register constantRegister = loadOperand(constant, Register.K1);
        Register nonConstantRegister = loadOperand(nonConstant, Register.K0);

        switch(op) {
            case ADD:
                // rd = nonConstant + constant
                text.add(new AluAsm(AluAsm.AluOp.addiu, rd, nonConstantRegister, constant.getValue()));
                break;

            case SUB:
                if (isOp1Constant) {
                    // rd = constant - nonConstant
                    // MIPS没有直接的减法立即数指令，需要使用subu
                    text.add(new AluAsm(AluAsm.AluOp.subu, rd, constantRegister, nonConstantRegister));
                } else {
                    // rd = nonConstant - constant
                    text.add(new AluAsm(AluAsm.AluOp.addiu, rd, nonConstantRegister, -constant.getValue()));
                }
                break;

            case MUL:
                // rd = nonConstant * constant (顺序不影响)
                text.add(new AluAsm(AluAsm.AluOp.mul, rd, nonConstantRegister, constantRegister));
                break;

            case SDIV:
                if (isOp1Constant) {
                    // rd = constant / nonConstant
                    text.add(new AluAsm(AluAsm.AluOp.div, constantRegister, nonConstantRegister));
                } else {
                    // rd = nonConstant / constant
                    text.add(new AluAsm(AluAsm.AluOp.div, nonConstantRegister, constantRegister));
                }
                text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
                break;

            case SREM:
                if (isOp1Constant) {
                    // rd = constant % nonConstant
                    text.add(new AluAsm(AluAsm.AluOp.div, constantRegister, nonConstantRegister));
                } else {
                    // rd = nonConstant % constant
                    text.add(new AluAsm(AluAsm.AluOp.div, nonConstantRegister, constantRegister));
                }
                text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
                break;

            default:
                // 处理其他操作或抛出异常
                throw new UnsupportedOperationException("Unsupported ALU operation: " + op);
        }
    }

    private void handleNoConstants(Alu.OP op, Register rd, Value op1,Value op2 ) {
        Register rs = loadOperand(op1, Register.getRegister(Register.K0.ordinal()));
        Register rt = loadOperand(op2, Register.getRegister(Register.K1.ordinal()));
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
        // 使用临时寄存器 $K0 计算偏移量
        Register tempReg = Register.getRegister(Register.K0.ordinal());

        // 生成指令：sp + offset -> $K0
        text.add(new AluAsm(AluAsm.AluOp.addiu, tempReg, Register.getRegister(Register.SP.ordinal()), getCurrentStackOffset()));

        // 将计算结果存储到栈中
        text.add(new Mem(Mem.MemOp.sw, getStackOffset(alloca), Register.getRegister(Register.SP.ordinal()), tempReg));
    }
//
//    //生成 BRANCH 指令的汇编代码
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
            if (op2 instanceof Constant) {
            } else {
                if (branchOp == BranchAsm.BranchOp.bgt) {
                    branchOp = BranchAsm.BranchOp.blt;
                } else if (branchOp == BranchAsm.BranchOp.ble) {
                    branchOp = BranchAsm.BranchOp.bge;
                } else if (branchOp == BranchAsm.BranchOp.blt) {
                    branchOp = BranchAsm.BranchOp.bgt;
                } else if (branchOp == BranchAsm.BranchOp.bge) {
                    branchOp = BranchAsm.BranchOp.ble;
                }
            }
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
            text.add(new BranchAsm(BranchAsm.BranchOp.beq, rd,1, currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
        } else {
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(icmp), Register.getRegister(Register.SP.ordinal()), Register.getRegister(Register.K0.ordinal())));
            text.add(new BranchAsm(BranchAsm.BranchOp.beq, Register.getRegister(Register.K0.ordinal()), 1, currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
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

    public void buildCall(Call call) {
        // 获取调用的函数
        Function function = (Function) call.getOperands().get(0);
        List<Value> params = call.getOperands().subList(1, call.getOperands().size());

        // 1. 保存所有活跃的寄存器和返回地址
        List<Register> activeRegs = getRegisters();

        List<Mem> saveInstructions = saveRegistersToStack(activeRegs);

        // 保存返回地址 ($ra)
        int raOffset = allocateStaticStackOffset(activeRegs.size());
        text.add(new Mem(Mem.MemOp.sw, raOffset, Register.SP, Register.RA));

        // 2. 处理参数传递
        handleCallParameters(params, activeRegs);
        // 为参数分配栈空间
        text.add(new AluAsm(AluAsm.AluOp.addiu, Register.SP, Register.SP, allocateStaticStackOffset(activeRegs.size())));
        // 3. 调用函数
        text.add(new JumpAsm(JumpAsm.JumpOp.jal, function.getRealName()));
        // 4. 恢复返回地址
        text.add(new Mem(Mem.MemOp.lw, 0, Register.SP, Register.RA));
        // 释放参数栈空间
        text.add(new AluAsm(AluAsm.AluOp.addiu, Register.SP, Register.SP, -allocateStaticStackOffset(activeRegs.size())));
        // 5. 恢复保存的寄存器
        restoreRegistersFromStack(activeRegs);

        // 6. 处理返回值
        handleReturnValue(call, function.getReturnType());
    }
    private List<Mem> saveRegistersToStack(List<Register> activeRegs) {
        List<Mem> instructions = new ArrayList<>();
        for (int i = 0; i < activeRegs.size(); i++) {
            int offset = allocateStaticStackOffset(i);
            Mem saveInstr = new Mem(Mem.MemOp.sw, offset, Register.SP, activeRegs.get(i));
            text.add(saveInstr);
            instructions.add(saveInstr);
        }
        return instructions;
    }
    private void restoreRegistersFromStack(List<Register> activeRegs) {
        for (int i = 0; i < activeRegs.size(); i++) {
            int offset = allocateStaticStackOffset(i);
            text.add(new Mem(Mem.MemOp.lw, offset, Register.SP, activeRegs.get(i)));
        }
    }
    private void handleCallParameters(List<Value> params, List<Register> activeRegs) {
        for (int i = 0; i < params.size(); i++) {
            Value param = params.get(i);
            if (i < 3) {
                // 前 3 个参数加载到 $a1, $a2, $a3
                Register paramReg = Register.getRegister(Register.A0.ordinal() + i + 1);
                if (param instanceof Constant) {
                    text.add(new Li(paramReg, ((Constant) param).getValue()));
                } else if (registerPool.containsKey(param)) {
                    if (param instanceof Param) {
                        // 从栈中加载到参数寄存器
                        int paramOffset = allocateStaticStackOffset(activeRegs.indexOf(registerPool.get(param)));
                        text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, paramReg));
                    } else {
                        // 从源寄存器移动到参数寄存器
                        text.add(new MoveAsm(paramReg, getRegister(param)));
                    }
                } else {
                    // 从栈中加载到参数寄存器
                    int paramOffset = getStackOffset(param);
                    text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, paramReg));
                }
            } else {
                // 超出 3 个的参数通过栈传递
                Register tempReg = Register.getRegister(Register.K0.ordinal());
                if (param instanceof Constant) {
                    // 如果参数是常量，直接加载到临时寄存器
                    text.add(new Li(tempReg, ((Constant) param).getValue()));
                } else if (registerPool.containsKey(param)) {
                    if (param instanceof Param) {
                        // 从栈中加载到参数寄存器
                        int paramOffset = allocateStaticStackOffset(activeRegs.indexOf(registerPool.get(param)));
                        text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, tempReg));
                    } else {
                        tempReg = getRegister(param);
                    }
                } else {
                    // 如果参数不在寄存器池中，从栈中加载到临时寄存器
                    int paramOffset = getStackOffset(param);
                    text.add(new Mem(Mem.MemOp.lw, paramOffset, Register.SP, tempReg));
                }
                // 将临时寄存器中的值存储到栈中
                int stackOffset = allocateStaticStackOffset(activeRegs.size() + i + 1);
                //activeRegs.size()：保存的活跃寄存器数量。
                //i：当前参数的索引（从 0 开始）。
                //+1：补偿保存的返回地址所占的一个栈位置。
                text.add(new Mem(Mem.MemOp.sw, stackOffset, Register.SP, tempReg));
            }
        }
    }

    private void handleReturnValue(Call call, LLVMType returnType) {
        if (returnType.isVoid()) {
            return; // 无返回值，不处理
        }
        Register destReg = getRegister(call);
        if (destReg != null) {
            // 将返回值从 $V0 移动到目标寄存器
            text.add(new AluAsm(AluAsm.AluOp.addiu, destReg, Register.V0, 0));
        } else {
            // 将返回值存储到栈中
            int retOffset = getStackOffset(call);
            text.add(new Mem(Mem.MemOp.sw, retOffset, Register.SP, Register.V0));
        }
    }
    private int allocateStaticStackOffset(int index) {
        return currentStackOffset - (index + 1) * 4; // 每次分配 4 字节，静态偏移
    }
    // 生成 GETPTR 指令的汇编代码
    // 生成 GETPTR 指令的汇编代码
    public void buildGetPtr(GetPtr getPtr) {
        // 获取基地址和偏移量
        Value base = getPtr.getOperands().get(0);  // 指针的基地址
        Value offset = getPtr.getOperands().get(1); // 指针的偏移量

        // 初始化寄存器
        Register baseReg = Register.getRegister(Register.K0.ordinal());   // 默认基址寄存器 $K0
        Register offsetReg = Register.getRegister(Register.K1.ordinal()); // 偏移量中间寄存器 $K1
        Register resultReg = Register.getRegister(Register.K0.ordinal()); // 结果寄存器 $K0

        // 确定基址寄存器
        if (base instanceof GlobalVar) {
            // 如果基址是全局变量，加载全局地址到寄存器
            text.add(new La(baseReg, base.getRealName())); // 去掉全局变量前缀
        } else if (registerPool.containsKey(base)) {
            // 基址已分配寄存器，直接使用
            baseReg = registerPool.get(base);
        } else {
            // 基址存储在栈中，从栈加载到寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(base), Register.SP, baseReg));
        }

        // 处理偏移量
        if (offset instanceof Constant) {
            // 偏移量为常量
            int scaledOffset = ((Constant) offset).getValue() * 4; // 偏移量乘以 4
            if (registerPool.containsKey(getPtr)) {
                // 目标寄存器已分配
                text.add(new AluAsm(AluAsm.AluOp.addiu, registerPool.get(getPtr), baseReg, scaledOffset));
            } else {
                // 使用结果寄存器存储
                text.add(new AluAsm(AluAsm.AluOp.addiu, resultReg, baseReg, scaledOffset));
                text.add(new Mem(Mem.MemOp.sw, getStackOffset(getPtr), Register.SP, resultReg));
            }
        } else {
            // 偏移量为变量
            if (registerPool.containsKey(offset)) {
                // 偏移量已分配寄存器
                offsetReg = registerPool.get(offset);
            } else {
                // 从栈加载偏移量到中间寄存器 $K1
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(offset), Register.SP, offsetReg));
            }

            // 左移偏移量寄存器 offsetReg (K1)，确保字节对齐
            text.add(new AluAsm(AluAsm.AluOp.sll, Register.K1, offsetReg, 2)); // sll $K1, $offsetReg, 2

            if (registerPool.containsKey(getPtr)) {
                // 如果目标寄存器已分配
                text.add(new AluAsm(AluAsm.AluOp.addu, registerPool.get(getPtr), baseReg, Register.K1)); // addu target, base, $K1
            } else {
                // 使用结果寄存器存储
                text.add(new AluAsm(AluAsm.AluOp.addu, resultReg, baseReg, Register.K1)); // addu $resultReg, base, $K1
                text.add(new Mem(Mem.MemOp.sw, getStackOffset(getPtr), Register.SP, resultReg));
            }
        }
    }



    // 生成 GETPTR 指令的汇编代码
//    public void buildGetPtr(GetPtr getPtr) {
//        // 获取指针的基地址和偏移量
//        Value base = getPtr.getOperands().get(0);
//        Value offset = getPtr.getOperands().get(1);
//
//        // 确定目标寄存器（默认使用 $K0）
//        Register targetReg = Register.getRegister(Register.K0.ordinal());
//
//        // 处理基地址，获取基地址所在的寄存器
//        Register baseReg = determineBaseRegister(base);
//
//        // 处理偏移量，根据偏移量的类型（常量或变量）进行不同处理
//        processOffset(getPtr, baseReg, offset, targetReg);
//    }

    // 确定基地址所在的寄存器
    private Register determineBaseRegister(Value base) {
        Register baseReg = Register.getRegister(Register.K0.ordinal());

        if (base instanceof GlobalVar) {
            // 如果基地址是全局变量，加载其地址到 baseReg 寄存器
            text.add(new La(baseReg, base.getRealName()));
        } else if (registerPool.containsKey(base)) {
            // 如果基地址已被分配到寄存器，直接使用该寄存器
            baseReg = getRegister(base);
        } else {
            // 否则，从栈中加载基地址的值到 baseReg 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(base), Register.SP, baseReg));
        }

        return baseReg;
    }

    // 处理偏移量，根据其是否为常量进行不同操作
    private void processOffset(GetPtr getPtr, Register baseReg, Value offset, Register targetReg) {
        Register offsetReg = Register.getRegister(Register.K1.ordinal());

        if (offset instanceof Constant) {
            // 处理常量偏移量
            handleConstantOffset(getPtr, baseReg, (Constant) offset, targetReg, offsetReg);
        } else {
            // 处理变量偏移量
            handleVariableOffset(getPtr, baseReg, offset, targetReg, offsetReg);
        }
    }

    // 处理常量偏移量
    private void handleConstantOffset(GetPtr getPtr, Register baseReg, Constant offset, Register targetReg, Register tempReg) {
        // 计算偏移量（乘以4以确保四字节对齐）
        int scaledOffset = offset.getValue() * 4;

        if (registerPool.containsKey(getPtr)) {
            // 如果目标寄存器已被分配，直接使用该寄存器
            targetReg = getRegister(getPtr);
            // 使用 addiu 指令将基地址寄存器与偏移量相加，结果存储到目标寄存器中
            text.add(new AluAsm(AluAsm.AluOp.addiu, targetReg, baseReg, scaledOffset));
        } else {
            // 使用临时寄存器存储结果
            text.add(new AluAsm(AluAsm.AluOp.addiu, tempReg, baseReg, scaledOffset));
            // 将临时寄存器的值存储到栈中指定的位置
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(getPtr), Register.SP, tempReg));
        }
    }

    // 处理变量偏移量
    private void handleVariableOffset(GetPtr getPtr, Register baseReg, Value offset, Register targetReg, Register tempReg) {
        if (registerPool.containsKey(offset)) {
            // 如果偏移量已被分配到寄存器，直接使用该寄存器
            tempReg = getRegister(offset);
        } else {
            // 否则，从栈中加载偏移量的值到 tempReg 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(offset), Register.SP, tempReg));
        }

        // 将偏移量左移2位（相当于乘以4），确保四字节对齐
        text.add(new AluAsm(AluAsm.AluOp.sll, tempReg, tempReg, 2));

        if (registerPool.containsKey(getPtr)) {
            // 如果目标寄存器已被分配，直接使用该寄存器
            targetReg = getRegister(getPtr);
            // 使用 addu 指令将基地址寄存器与缩放后的偏移量相加，结果存储到目标寄存器中
            text.add(new AluAsm(AluAsm.AluOp.addu, targetReg, baseReg, tempReg));
        } else {
            // 使用 addu 指令将基地址寄存器与缩放后的偏移量相加，结果存储到临时寄存器中
            text.add(new AluAsm(AluAsm.AluOp.addu, tempReg, baseReg, tempReg));
            // 将临时寄存器的值存储到栈中指定的位置
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(getPtr), Register.SP, tempReg));
        }
    }



    // 生成 ICMP 指令的汇编代码
    public void buildIcmp(Icmp icmp) {
        // 如果是控制流相关的 icmp 指令，直接返回
        if (icmp.isControlFlow()) {
            return;
        }

        // 获取操作数和操作符
        Value op1 = icmp.getOperands().get(0);
        Value op2 = icmp.getOperands().get(1);
        Icmp.OP op = icmp.getOp();
        // 确定目标寄存器
        Register rd = registerPool.containsKey(icmp)
                ? getRegister(icmp)
                : Register.getRegister(Register.K0.ordinal());

        // 将 LLVM 的比较操作映射到汇编层面的比较操作
        CmpAsm.CmpOp cmpOp = mapIcmpOpToCmpAsmOp(op);

        // 加载操作数到寄存器
        Register rs = loadOperand(op1, Register.getRegister(Register.K0.ordinal()));
        Register rt = loadOperand(op2, Register.getRegister(Register.K1.ordinal()));

        // 生成比较指令
        text.add(new CmpAsm(cmpOp, rd, rs, rt));//记住寄存器参数的顺序！！！找了很久

        // 如果目标寄存器是临时寄存器，则将结果存储到栈中
        if (!registerPool.containsKey(icmp)) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(icmp), Register.SP, rd));
        }
    }

    // 将 LLVM 的 Icmp 操作符映射到汇编的比较操作符
    private CmpAsm.CmpOp mapIcmpOpToCmpAsmOp(Icmp.OP op) {
        switch (op) {
            case EQ:
                return CmpAsm.CmpOp.seq;
            case NE:
                return CmpAsm.CmpOp.sne;
            case SLT:
                return CmpAsm.CmpOp.slt;
            case SLE:
                return CmpAsm.CmpOp.sle;
            case SGT:
                return CmpAsm.CmpOp.sgt;
            case SGE:
                return CmpAsm.CmpOp.sge;
            default:
                throw new IllegalArgumentException("Unsupported Icmp operation: " + op);
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
            // 如果指针是全局变量，加载其地址到 K0 寄存器
            text.add(new La(ptrReg, ptr.getRealName()));
        } else if (registerPool.containsKey(ptr)) {
            // 如果指针已被分配到寄存器，直接使用该寄存器
            ptrReg = getRegister(ptr);
        } else {
            // 否则，从栈中加载指针的值到 K0 寄存器
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
            // 如果指针是全局变量，加载其地址到 K0 寄存器
            text.add(new La(ptrReg, ptr.getRealName()));
        } else if (registerPool.containsKey(ptr)) {
            // 如果指针已被分配到寄存器，直接使用该寄存器
            ptrReg = getRegister(ptr);
        } else {
            // 否则，从栈中加载指针的值到 K0 寄存器
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
            // 否则，从栈中加载值到 K1 寄存器
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
    public void buildZext(Zext zextInstr) {
        Value value = zextInstr.getOperands().get(0);
        Register reg = Register.K0;
        if (value instanceof Constant) {
            new Li(reg, ((Constant) value).getValue());
        } else if (registerPool.containsKey(value)) {
            reg = registerPool.get(value);
        } else {
            text.add(new Mem(Mem.MemOp.lw, stackOffset.get(value), Register.SP, Register.K0));
        }
        if (registerPool.containsKey(zextInstr)) { // zextInstr is a register
//            new AluAsm(reg, null, AluAsm.AluOp.addiu, registerPool.get(zextInstr), 0); // move reg to zextInstr
            text.add(new AluAsm(AluAsm.AluOp.addiu, registerPool.get(zextInstr), reg, 0));
        } else {
            text.add(new Mem(Mem.MemOp.sw, stackOffset.get(zextInstr), Register.SP, reg)); // store reg to zextInstr
        }
    }

    // 生成 TRUNC 指令的汇编代码
    public void buildTrunc(Trunc trunc) {
        // 获取源值和目标寄存器
        Value src = trunc.getOperands().get(0);
        Register rd = registerPool.containsKey(trunc)
                ? getRegister(trunc)
                : Register.getRegister(Register.K0.ordinal());

        // 加载源值到寄存器
        Register srcReg = loadOperand(src, Register.getRegister(Register.K0.ordinal()));

        // 生成 ANDI 指令，截断到低8位
        text.add(new AluAsm(AluAsm.AluOp.andi, rd, srcReg, 0xFF));

        // 如果目标寄存器是临时寄存器，则将结果存储到栈中
        if (!registerPool.containsKey(trunc)) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(trunc), Register.SP, rd));
        }
    }

    // 生成 RET 指令的汇编代码
    public void buildRet(Ret ret) {
        // 如果是主函数，生成 Li 指令加载立即数 10 到 V0 寄存器，然后生成 syscall 指令
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
        if (val instanceof Constant) {
            // 如果返回值是常量，使用 li 指令加载立即数
            text.add(new Li(Register.V0, ((Constant) val).getValue()));
        } else if (registerPool.containsKey(val)) {
            // 如果返回值已被分配到寄存器，直接使用该寄存器
            Register valReg = getRegister(val);
            text.add(new AluAsm(AluAsm.AluOp.addiu, Register.V0, valReg, 0));
        } else {
            // 否则，从栈中加载返回值到 K0 寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(val), Register.SP, Register.V0));
        }
        // 生成无条件跳转指令
        text.add(new JumpAsm(JumpAsm.JumpOp.jr, Register.RA));
    }

    // 生成 GetInt 指令的汇编代码
    public void buildGetInt(Getint getInt) {
        // 生成 li 指令加载立即数 5 到 V0 寄存器
        text.add(new Li(Register.V0, 5));
        // 生成 syscall 指令
        text.add(new Syscall());
        // 获取目标寄存器
        Register rd = getOrAllocateRegister(getInt);
        // 将返回值存储到目标寄存器
        text.add(new MoveAsm(rd, Register.V0));
        // 如果目标寄存器不是已分配寄存器，将结果存回栈
        storeToStackIfNeeded(getInt, rd);
    }

    // 生成 PutInt 指令的汇编代码
    public void buildPutInt(Putint putInt) {
        handleParameter(putInt.getOperands().get(0), Register.A0);
        // 生成 li 指令加载立即数 1 到 V0 寄存器
        text.add(new Li(Register.V0, 1));
        // 生成 syscall 指令
        text.add(new Syscall());
    }

    // 生成 PutStr 指令的汇编代码
    public void buildPutStr(Putstr putStr) {
        // 生成 la 指令加载字符串地址到 a0 寄存器
        text.add(new La(Register.A0, putStr.getConstStr().getRealName()));
        // 生成 li 指令加载立即数 4 到 V0 寄存器
        text.add(new Li(Register.V0, 4));
        // 生成 syscall 指令
        text.add(new Syscall());
    }

    // 生成 GetChar 指令的汇编代码
    public void buildGetChar(Getchar getchar) {
        // 生成 li 指令加载立即数 12 到 V0 寄存器
        text.add(new Li(Register.V0, 12));
        // 生成 syscall 指令
        text.add(new Syscall());
        // 获取目标寄存器
        Register rd = getOrAllocateRegister(getchar);
        // 将返回值存储到目标寄存器
        text.add(new MoveAsm(rd, Register.V0));
        // 如果目标寄存器不是已分配寄存器，将结果存回栈
        storeToStackIfNeeded(getchar, rd);
    }

    // 生成 PutCh 指令的汇编代码
    public void buildPutCh(Putch putCh) {
        handleParameter(putCh.getOperands().get(0), Register.A0);
        // 生成 li 指令加载立即数 11 到 V0 寄存器
        text.add(new Li(Register.V0, 11));
        // 生成 syscall 指令
        text.add(new Syscall());
    }

    // 通用方法：处理参数
    private void handleParameter(Value val, Register targetReg) {
        if (val instanceof Constant) {
            // 如果参数是常量，使用 li 指令加载立即数
            text.add(new Li(targetReg, ((Constant) val).getValue()));
        } else if (registerPool.containsKey(val)) {
            // 如果参数已被分配到寄存器，直接使用该寄存器
            text.add(new MoveAsm(targetReg, getRegister(val)));
        } else {
            // 否则，从栈中加载参数到目标寄存器
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(val), Register.SP, targetReg));
        }
    }

    // 通用方法：获取或分配目标寄存器
    private Register getOrAllocateRegister(Value value) {
        return getRegister(value) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(value);
    }

    // 通用方法：根据需要将寄存器内容存储回栈
    private void storeToStackIfNeeded(Value value, Register reg) {
        if (!registerPool.containsKey(value)) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(value), Register.SP, reg));
        }
    }
    public void buildMove(Move move) {
        Value to = move.getTo();
        Value from = move.getFrom();
        Register toReg = Register.K0;
        if (registerPool.containsKey(to)) {
            toReg = registerPool.get(to);
        }
        if (from instanceof Constant) {
            text.add(new Li(toReg, ((Constant) from).getValue()));
        } else if (registerPool.containsKey(from)) {
            text.add(new MoveAsm(toReg, registerPool.get(from)));
        } else {
            text.add(new Mem(Mem.MemOp.lw, getStackOffset(from), Register.SP, toReg));
        }
        if (toReg == Register.K0) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(to), Register.SP, toReg));
        }
    }

    // 生成所有的汇编代码
    public void mipsBuilder(Module module) {
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
