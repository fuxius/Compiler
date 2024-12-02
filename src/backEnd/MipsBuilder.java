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

    //生成常量字符串的汇编代码
    private int constStrCount = 0; // 类成员变量，用于生成唯一标签

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

        if (type.isInt32()) { // 处理 int 类型
            if (globalVar.isZeroInitial()) {
                if (globalVar.getLen() > 0) {
                    // 未初始化的 int 数组，使用 .word 0:<count>
                    int count = globalVar.getLen();
                    data.add(new WordAsm(name, Collections.nCopies(count, 0)));
                } else {
                    // 单个未初始化的 int 变量，使用 .word 0
                    data.add(new WordAsm(name, Collections.singletonList(0)));
                }
            } else if (globalVar.getLen() > 0) {
                // 初始化的 int 数组，使用 .word 并列出所有初值
                List<Integer> initial = globalVar.getInitial();
                data.add(new WordAsm(name, initial));
            } else {
                // 单个初始化的 int 变量
                List<Integer> initial = globalVar.getInitial();
                data.add(new WordAsm(name, initial));
            }
        } else if (type.isInt8()) { // 处理 char 类型
            if (globalVar.isZeroInitial()) {
                if (globalVar.getLen() > 0) {
                    // 未初始化的 char 数组，使用 .byte 0:<count>
                    int count = globalVar.getLen();
                    data.add(new ByteAsm(name, count));
                } else {
                    // 单个未初始化的 char 变量，使用 .byte 0
                    data.add(new ByteAsm(name, 0));
                }
            } else if (globalVar.getLen() > 0) {
                // 初始化的 char 数组，使用 .byte 并列出所有初值
                List<Integer> initial = globalVar.getInitial();
                data.add(new ByteAsm(name, initial));
            } else {
                // 单个初始化的 char 变量
                List<Integer> initial = globalVar.getInitial();
                data.add(new ByteAsm(name, initial));
            }
        }
    }

    //初始化函数的上下文，包括当前函数、栈偏移、变量与寄存器的映射等
    public void initFunction(Function function) {
        setCurrentFunction(function);
        setCurrentStackOffset(0);
        registerPool = function.getRegisterPool();
        stackOffset = new HashMap<>();
    }

    //生成函数的汇编代码
    public void buildFunction(Function function) {
        //生成函数标签
        LableAsm lableAsm = new LableAsm(function.getRealName()+":",false);
        text.add(lableAsm);
        //初始化函数的上下文
        initFunction(function);

        //处理函数参数,遍历函数参数并分配栈偏移量或寄存器.可以为前3个参数分配a1-a3寄存器，但仍然在桟里为其预留空间
        List<Param> params = function.getParams();
        for (int i = 0; i < params.size(); i++) {
            decreaseStackOffset(4);
            Param param = params.get(i);
            stackOffset.put(param, getCurrentStackOffset());
            if (i < 3) {
                Register register = Register.getRegister(Register.A0.ordinal() + i + 1);
                registerPool.put(param, register);
            }
        }
        //遍历函数的所有基本块中的指令，分配栈偏移量
        List<BasicBlock> basicBlocks = function.getBasicBlocks();
        for (BasicBlock basicBlock : basicBlocks) {
            List<Instruction> instructions = basicBlock.getInstrs();
            for (Instruction instruction : instructions) {
                if (instruction.hasLVal()) {
                    //检查指令是否有LVal并且未分配寄存器和栈偏移量
                    if (!stackOffset.containsKey(instruction) && !registerPool.containsKey(instruction)) {
                        decreaseStackOffset(4);
                        stackOffset.put(instruction, getCurrentStackOffset());
                    }
                }
                //TODO:处理Move指令
            }
        }

        //遍历函数的所有基本块并生成对应的汇编指令
        for (BasicBlock basicBlock : basicBlocks) {
            buildBasicBlock(basicBlock);
        }
    }

    //生成基本块的汇编代码
    public void buildBasicBlock(BasicBlock basicBlock) {
        //生成基本块标签
        LableAsm lableAsm = new LableAsm("\t"+basicBlock.getParentFunc().getRealName()+"_"+basicBlock.getName(),true);
        text.add(lableAsm);
        //遍历基本块中的指令并生成对应的汇编指令
        List<Instruction> instructions = basicBlock.getInstrs();
        for (Instruction instruction : instructions) {
            buildInstruction(instruction);
        }
    }

    //生成指令的汇编代码
    public void buildInstruction(Instruction instruction) {
        //分情况讨论
//        public static enum InstrType {
//            ALU, ALLOCA, BRANCH, CALL, GETPTR, ICMP, JUMP, LOAD, RETURN, STORE, ZEXT,
//            GETINT, PUTSTR, PUTINT, PHI, MOVE, GETCHAR, PUTCH, TRUNC
//        }
        switch (instruction.getInstrType()) {
            case ALU:
                text.add(new Comment(instruction));
                buildAlu((Alu) instruction);
                break;
            case ALLOCA:
                text.add(new Comment(instruction));
                buildAlloca((Alloca) instruction);
                break;
            case BRANCH:
                text.add(new Comment(instruction));
                buildBranch((Branch) instruction);
                break;
            case CALL:
                text.add(new Comment(instruction));
                buildCall((Call) instruction);
                break;
            case GETPTR:
                text.add(new Comment(instruction));
                buildGetPtr((GetPtr) instruction);
                break;
            case ICMP:
                text.add(new Comment(instruction));
                buildIcmp((Icmp) instruction);
                break;
            case LOAD:
                text.add(new Comment(instruction));
                buildLoad((Load) instruction);
                break;
            case RETURN:
                text.add(new Comment(instruction));
                buildRet((Ret) instruction);
                break;
            case STORE:
                text.add(new Comment(instruction));
                buildStore((Store) instruction);
                break;
            case ZEXT:
                text.add(new Comment(instruction));
                buildZext((Zext) instruction);
                break;
            case TRUNC:
                text.add(new Comment(instruction));
                buildTrunc((Trunc) instruction);
                break;
            case GETINT:
                text.add(new Comment(instruction));
                buildGetInt((Getint) instruction);
                break;
            case PUTSTR:
                text.add(new Comment(instruction));
                buildPutStr((Putstr) instruction);
                break;
            case PUTINT:
                text.add(new Comment(instruction));
                buildPutInt((Putint) instruction);
                break;
            case GETCHAR:
                text.add(new Comment(instruction));
                buildGetChar((Getchar) instruction);
                break;
            case PUTCH:
                text.add(new Comment(instruction));
                buildPutCh((Putch) instruction);
                break;
            default:
                break;
        }
    }

    //生成 ALU 指令的汇编代码
    public void buildAlu(Alu alu) {
        //获取操作数和操作符
        Value op1 = alu.getOperands().get(0);
        Value op2 = alu.getOperands().get(1);
        Alu.OP op = alu.getOp();
        //确定常量操作数的数量
        int constNum = 0;
        if (op1 instanceof Constant) {
            constNum++;
        }
        if (op2 instanceof Constant) {
            constNum++;
        }
        //确定目标寄存器
        Register rd = getRegister(alu) == null ? Register.getRegister(Register.K0.ordinal()) : getRegister(alu);
        //分情况讨论
        if (constNum == 2) {
            //如果两个操作数都是常量，则将它们加载到寄存器中并进行操作
            Register rs = Register.getRegister(Register.K0.ordinal());
            Register rt = Register.getRegister(Register.K1.ordinal());
            //加载常数到寄存器
            Li li1 = new Li(rs, ((Constant) op1).getValue());
            text.add(li1);
            //如果操作符不是加法或减法，将第二个常数加载到寄存器 k1 中
            if (op != Alu.OP.ADD && op != Alu.OP.SUB) {
                Li li2 = new Li(rt, ((Constant) op2).getValue());
                text.add(li2);
                if(op == Alu.OP.MUL){
                    text.add(new AluAsm(AluAsm.AluOp.mul, rd, rs, rt, 0));
                }else if(op == Alu.OP.SDIV) {
                    text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
                }else if(op == Alu.OP.SREM) {
                    text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
                }
            }else {
                //如果操作符是加法或减法，计算第二个常数的值（如果是减法，取其负值）。
                int value = ((Constant) op2).getValue();
                if (op == Alu.OP.SUB) {
                    value = -value;
                }
                //生成加法立即数指令，将 k0 和常数相加，结果存储在目标寄存器
                text.add(new AluAsm(AluAsm.AluOp.addiu, rd, rs, value));
            }
        } else if (constNum == 1) {
            //情况 1：第一个操作数是常量
            if(op1 instanceof Constant) {
                //如果第一个操作数是常量，将第二个操作数加载到寄存器 k0 中，而不是第一个操作数，为什么？ 因为第一个操作数是常量，不需要保存
                Register rs = Register.getRegister(Register.K0.ordinal());
                if(registerPool.containsKey(op2)) {
                    rs = getRegister(op2);
                }else {
                    text.add(new Mem(Mem.MemOp.lw, getStackOffset(op2), Register.getRegister(Register.SP.ordinal()), rs));
                }
                if (op != Alu.OP.ADD && op != Alu.OP.SUB) {
                    //如果操作符不是加法或减法，将第一个常数加载到寄存器 k1 中
                    Register rt = Register.getRegister(Register.K1.ordinal());
                    Li li = new Li(rt, ((Constant) op1).getValue());
                    text.add(li);
                    if(op == Alu.OP.MUL){
                        text.add(new AluAsm(AluAsm.AluOp.mul, rd, rs, rt, 0));
                    }else if(op == Alu.OP.SDIV) {
                        text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
                    }else if(op == Alu.OP.SREM) {
                        text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
                    }
                }else {
                    //如果操作符是加法
                    int value = ((Constant) op1).getValue();
                    if (op == Alu.OP.ADD) {
                        //生成加法立即数指令，将 k0 和常数相加，结果存储在目标寄存器
                        text.add(new AluAsm(AluAsm.AluOp.addiu, rd, rs, value));
                    } else {
                        //如果操作符是减法，生成减法立即数指令，将 k0 和常数相减，结果存储在目标寄存器
                        text.add(new AluAsm(AluAsm.AluOp.subu, rd, rs, value));
                    }
                }

            } else {
                //情况 2：第二个操作数是常量
                //如果第二个操作数是常量，将第一个操作数加载到寄存器 k0 中，而不是第二个操作数，为什么？ 因为第二个操作数是常量，不需要保存
                Register rs = Register.getRegister(Register.K0.ordinal());
                if(registerPool.containsKey(op1)) {
                    rs = getRegister(op1);
                }else {
                    text.add(new Mem(Mem.MemOp.lw, getStackOffset(op1), Register.getRegister(Register.SP.ordinal()), rs));
                }
                if (op != Alu.OP.ADD && op != Alu.OP.SUB) {
                    //如果操作符不是加法或减法，将第二个常数加载到寄存器 k1 中
                    Register rt = Register.getRegister(Register.K1.ordinal());
                    Li li = new Li(rt, ((Constant) op2).getValue());
                    text.add(li);
                    if(op == Alu.OP.MUL){
                        text.add(new AluAsm(AluAsm.AluOp.mul, rd, rs, rt, 0));
                    }else if(op == Alu.OP.SDIV) {
                        text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
                    }else if(op == Alu.OP.SREM) {
                        text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
                    }
                }else {
                    //如果操作符是加法
                    int value = ((Constant) op2).getValue();
                    if (op == Alu.OP.ADD) {
                        //生成加法立即数指令，将 k0 和常数相加，结果存储在目标寄存器
                        text.add(new AluAsm(AluAsm.AluOp.addiu, rd, rs, value));
                    } else {
                        //如果操作符是减法，生成减法立即数指令，将 k0 和常数相减，结果存储在目标寄存器
                        text.add(new AluAsm(AluAsm.AluOp.subu, rd, rs, value));
                    }
                }
            }
        }else {
            //两个操作数都不是常量
            Register rs = getRegister(op1);
            Register rt = getRegister(op2);
            if (rs == null) {
                rs = Register.getRegister(Register.K0.ordinal());
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(op1), Register.getRegister(Register.SP.ordinal()), rs));
            }
            if (rt == null) {
                rt = Register.getRegister(Register.K1.ordinal());
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(op2), Register.getRegister(Register.SP.ordinal()), rt));
            }
            if(op == Alu.OP.ADD){
                text.add(new AluAsm(AluAsm.AluOp.addu, rd, rs, rt, 0));
            }else if(op == Alu.OP.SUB){
                text.add(new AluAsm(AluAsm.AluOp.subu, rd, rs, rt, 0));
            }
            else if(op == Alu.OP.MUL){
                text.add(new AluAsm(AluAsm.AluOp.mul, rd, rs, rt, 0));
            }else if(op == Alu.OP.SDIV) {
                text.add(new AluAsm(AluAsm.AluOp.div, rs, rt, 0));
                text.add(new MoveFrom(MoveFrom.Type.MFLO, rd));
            }else if(op == Alu.OP.SREM) {
                text.add(new AluAsm(AluAsm.AluOp.div, rs, rt, 0));
                text.add(new MoveFrom(MoveFrom.Type.MFHI, rd));
            }
        }
        //如果目标寄存器是Register.K0，则将结果存回内存
        if (rd == Register.getRegister(Register.K0.ordinal())) {
            text.add(new Mem(Mem.MemOp.sw, getStackOffset(alu), Register.getRegister(Register.SP.ordinal()), rd));
        }
    }

    //生成 ALLOCA 指令的汇编代码
    public void buildAlloca(Alloca alloca) {
        //获取指向类型
        LLVMType type = alloca.getPointedType();
        //根据类型更新栈偏移量
        if (type.isArray()) {
            //如果指向的类型是数组类型，栈偏移量增加数组长度乘以 4
            ArrayType arrayType = (ArrayType) type;
            decreaseStackOffset(arrayType.getLength() * 4);
        } else {
            //如果指向的类型不是数组类型，栈偏移量增加 4
            decreaseStackOffset(4);
        }
        //根据寄存器池种是否包含Alloca,生成汇编指令
        if (registerPool.containsKey(alloca)) {
            //如果 registerPool 包含 Alloca 指令，则创建一个新的 AluAsm 指令，将当前偏移量加到栈指针（Register.sp）上，并将结果存储在与 Alloca 指令关联的寄存器中。
            Register rd = getRegister(alloca);
            text.add(new AluAsm(AluAsm.AluOp.addiu, rd, Register.getRegister(Register.SP.ordinal()), getCurrentStackOffset()));
        } else {
            //如果 registerPool 不包含 Alloca 指令，则创建一个新的 Mem 指令，将当前偏移量存储在栈中。
            text.add(new AluAsm(AluAsm.AluOp.addiu, Register.getRegister(Register.K0.ordinal()), Register.getRegister(Register.SP.ordinal()), getCurrentStackOffset()));
            text.add(new Mem(Mem.MemOp.sw,getStackOffset(alloca), Register.getRegister(Register.SP.ordinal()), Register.getRegister(Register.K0.ordinal())));
        }

    }

    //生成 BRANCH 指令的汇编代码
    public void buildBranch(Branch branch) {
        //如果是无条件分支，直接生成 JumpAsm 指令
        if (!branch.isConditional()) {
            text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName()+"_"+branch.getParentBlock().getName()));
            return;
        }
        //获取条件分支的第一个操作数
        Icmp icmp = (Icmp) branch.getOperands().get(0);
        //检查这个 icmp 是否是一个仅用于分支判断的指令
        if(!icmp.isControlFlow()){
            //如果不是，则将 icmp 的结果存储在寄存器中
            Register rd = getRegister(icmp);
            if (rd != null) {
                //如果 icmp 有对应的寄存器映射，则直接使用该寄存器生成一个 BranchAsm 指令。
                Register rs = Register.getRegister(Register.K0.ordinal());
                text.add(new BranchAsm(BranchAsm.BranchOp.beq, rd, rs, currentFunction.getRealName()+"_"+branch.getElseBlock().getName()));
            } else {
                //如果 icmp 没有对应的寄存器映射，则生成一个 Mem 指令，将 icmp 的结果存储在栈中。
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(icmp), Register.getRegister(Register.SP.ordinal()), Register.getRegister(Register.K0.ordinal())));
                text.add(new BranchAsm(BranchAsm.BranchOp.beq, null, Register.getRegister(Register.K0.ordinal()), 1,currentFunction.getRealName()+"_"+branch.getElseBlock().getName()));
            }
            //处理常规的条件分支,跳转到 else 块
            text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName()+"_"+branch.getElseBlock().getName()));
            return;
        }
        //根据 icmp 中的操作符，设置相应的汇编分支操作符
        Icmp.OP op = icmp.getOp();
        BranchAsm.BranchOp branchOp = null;
        switch (op) {
            case EQ:
                branchOp = BranchAsm.BranchOp.beq;
                break;
            case NE:
                branchOp = BranchAsm.BranchOp.bne;
                break;
            case SLT:
                branchOp = BranchAsm.BranchOp.blt;
                break;
            case SLE:
                branchOp = BranchAsm.BranchOp.ble;
                break;
            case SGT:
                branchOp = BranchAsm.BranchOp.bgt;
                break;
            case SGE:
                branchOp = BranchAsm.BranchOp.bge;
                break;
            default:
                break;
        }
        //获取 icmp 的操作数
        Value op1 = icmp.getOperands().get(0);
        Value op2 = icmp.getOperands().get(1);
        //确定常量操作数的数量
        int constNum = 0;
        if (op1 instanceof Constant) {
            constNum++;
        }
        if (op2 instanceof Constant) {
            constNum++;
        }
        //分情况讨论
        if (constNum == 2) {
            //如果两个操作数都是常量，则将它们加载到寄存器中并进行比较
            Register rs = Register.getRegister(Register.K0.ordinal());
            //加载常数到寄存器
            Li li1 = new Li(rs, ((Constant) op1).getValue());
            text.add(li1);
            //生成比较指令
            text.add(new BranchAsm(branchOp, rs, ((Constant) op2).getValue(),currentFunction.getRealName()+"_"+branch.getThenBlock().getName()));
        } else if (constNum == 1) {
            //情况 1：第一个操作数是常量
            if (op1 instanceof Constant) {
                //如果第一个操作数是常量，将第二个操作数加载到寄存器 k0 中，而不是第一个操作数，为什么？ 因为第一个操作数是常量，不需要保存
                Register rs = Register.getRegister(Register.K0.ordinal());
                if (registerPool.containsKey(op2)) {
                    rs = getRegister(op2);
                } else {
                    text.add(new Mem(Mem.MemOp.lw, getStackOffset(op2), Register.getRegister(Register.SP.ordinal()), rs));
                }
                //生成比较指令
                text.add(new BranchAsm(branchOp, rs, ((Constant) op1).getValue(), currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
            } else {
                //情况 2：第二个操作数是常量
                //如果第二个操作数是常量，将第一个操作数加载到寄存器 k0 中，而不是第二个操作数，为什么？ 因为第二个操作数是常量，不需要保存
                Register rs = Register.getRegister(Register.K0.ordinal());
                if (registerPool.containsKey(op1)) {
                    rs = getRegister(op1);
                } else {
                    text.add(new Mem(Mem.MemOp.lw, getStackOffset(op1), Register.getRegister(Register.SP.ordinal()), rs));
                }
                //生成比较指令
                text.add(new BranchAsm(branchOp, rs, ((Constant) op2).getValue(), currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
            }
        }else {
            //两个操作数都不是常量
            Register rs = getRegister(op1);
            Register rt = getRegister(op2);
            if (rs == null) {
                rs = Register.getRegister(Register.K0.ordinal());
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(op1), Register.getRegister(Register.SP.ordinal()), rs));
            }
            if (rt == null) {
                rt = Register.getRegister(Register.K1.ordinal());
                text.add(new Mem(Mem.MemOp.lw, getStackOffset(op2), Register.getRegister(Register.SP.ordinal()), rt));
            }
            text.add(new BranchAsm(branchOp, rs, rt, currentFunction.getRealName() + "_" + branch.getThenBlock().getName()));
        }
        //处理常规的条件分支,跳转到 else 块
        text.add(new JumpAsm(JumpAsm.JumpOp.j, currentFunction.getRealName()+"_"+branch.getElseBlock().getName()));
    }

    // 生成 CALL 指令的汇编代码
    public void buildCall(Call call) {
        // 获取调用的函数
        Function function = (Function) call.getOperands().get(0);
        // 获取参数列表
        List<Value> params = call.getOperands().subList(1, call.getOperands().size());

        // 1. 寄存器分配
        // 获取寄存器池中的所有寄存器 Hashset
        ArrayList<Register> registers = getRegisters();
        // TODO:优化：仅保存 callInstr 中活跃使用的寄存器，并确保唯一性

        // 2. 初始化保存和恢复指令列表
        ArrayList<Mem> loadAsms = new ArrayList<>();   // 加载指令列表
        ArrayList<Mem> storeAsms = new ArrayList<>();  // 存储指令列表

        // 3. 确保特定的参数寄存器（a1, a2, a3）被保存
        for (Register reg : registerPool.values()) {
            if (reg == Register.A1 || reg == Register.A2 || reg == Register.A3) {
                if (!registers.contains(reg)) {
                    registers.add(reg);
                }
            }
        }

        // 4. 为保存的寄存器生成存储指令
        for (int i = 0; i < registers.size(); i++) {
            Register reg = registers.get(i);
            // 计算寄存器在栈中的存储位置
            int offset = getCurrentStackOffset() - (i + 1) * 4;
            // 生成 store word (sw) 指令，将寄存器的值存储到栈中
            storeAsms.add(new Mem(Mem.MemOp.sw, offset, Register.SP, reg));
        }
        // 保存返回地址（ra）到栈中
        int raOffset = getCurrentStackOffset() - (registers.size() + 1) * 4;
        storeAsms.add(new Mem(Mem.MemOp.sw, raOffset, Register.SP, Register.RA));

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
            loadAsms.add(new Mem(Mem.MemOp.lw, offset, Register.SP, reg));
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
        // 生成 trunc 指令，将源值截断为目标类型
        text.add(new AluAsm(AluAsm.AluOp.and, rd, srcReg, 0xFFFF));
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
        // 生成 zext 指令，将源值零扩展为目标类型
        text.add(new AluAsm(AluAsm.AluOp.and, rd, srcReg, 0xFFFF));
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
