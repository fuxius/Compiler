package midEnd;
import LLVMIR.Base.Core.Module;
import LLVMIR.Global.Function;
import midEnd.Loop.LoopAnalysis;
import midEnd.Loop.MemoryAccessOptimize;
import midEnd.Var.GCM;
import midEnd.Var.GVN;
import midEnd.Var.GlobalVarLocalize;
import midEnd.base.ActiveVarAnalyzer;
import midEnd.base.CFGBuilder;
import midEnd.base.RegAlloc;
import midEnd.helper.SideEffectsAnalyze;
import midEnd.helper.delete;
import midEnd.mem.MemToReg;
import midEnd.mem.Remove;

public class Optimizer {
    // 单例模式
    private static Optimizer instance = new Optimizer();
    public static boolean againstLlvm = true;
    private Optimizer() {
    }

    public static Optimizer getInstance() {
        return instance;
    }
    public  static boolean basicOptimize = false;

    public boolean isBasicOptimize() {
        return basicOptimize;
    }

    public void analyze(Module module) {
//         调用delete的simplify方法
        delete.simplify(module);
        // 再次调用CFGBuilder的run方法
        CFGBuilder.buildCFG(module);
        // 调用ActiveVarAnalyzer的analyze方法
        SideEffectsAnalyze.analyzeSideEffects(module);
        for (Function func : module.getFunctions()) {
            ActiveVarAnalyzer.analyze(func);
        }
        delete.simplify(module);
        LoopAnalysis.analyzeLoop(module);
    }

    public void run(Module module) {
        if (basicOptimize) {
            delete.simplify(module);
            CFGBuilder.buildCFG(module);
            MemToReg.execute(module);
            CFGBuilder.buildCFG(module);
            for (Function func : module.getFunctions()) {
                ActiveVarAnalyzer.analyze(func);
            }
            RegAlloc regAlloc = new RegAlloc();
            regAlloc.allocateRegisters(module);
            Remove.removePhi(module);
            delete.simplify(module);
        } else {
            for (int i = 1; i <= 10; i++) {
                analyze(module);
                // 调用GlobalVarLocalize的globalVarLocalize方法
                GlobalVarLocalize.globalVarLocalize(module);
                analyze(module);
                // 调用MemToReg的execute方法
                MemToReg.execute(module);
                analyze(module);
                // 调用GVN的execute方法
                GVN.optimize(module);
                analyze(module);
                delete.mergeBlocks(module);
                analyze(module);
                MemoryAccessOptimize.optimize(module);
                // 调用GCM的execute方法
                analyze(module);
                GCM.moveInstrs(module);
            }
            RegAlloc regAlloc = new RegAlloc();
            regAlloc.allocateRegisters(module);
            Remove.removePhi(module);
        }
    }
}
//函数活动记录分析
//一个典型函数的活动记录（从高地址到低地址）分为以下几个区域，分区及寄存器保存位置（以字为单位）如下：
//
//1. 参数区
//用于传递超过4个参数的栈空间。
//如果函数参数超过4个：
//前4个参数存放在 $a0-$a3 中。
//额外参数存放在 栈空间的 sp + 0 开始，每个参数占用 1 字（4 字节）。
//栈的这部分为被调用函数保留的空间。
//2. 保存区
//保存返回地址（$ra）、调用者保存的寄存器和被调用者保存的寄存器：
//返回地址 ($ra): 1 字。
//调用者保存的寄存器 ($t0-$t9)：如果函数内使用，按需保存，每个寄存器占 1 字。
//被调用者保存的寄存器 ($s0-$s7)：如果函数内使用，按需保存，每个寄存器占 1 字。
//3. 局部变量区
//非数组局部变量：每个变量占 1 字（4 字节）。
//数组变量：根据数组长度，所需空间为 数组长度 × 1 字。
//临时溢出变量存储区：当寄存器不够时，用于保存临时值，每个溢出变量占 1 字。
//4. 临时区
//为函数调用预留的参数传递空间：
//根据被调用函数的参数数量，预留 参数个数 × 1 字。
//选择优化：图着色寄存器分配
//基本思路
//图着色用于优化寄存器分配，旨在减少栈中变量的溢出，通过减少访存操作提升代码运行效率。
//实现步骤：
//构建变量的 冲突图：
//顶点表示变量。
//如果两个变量的生存区间重叠，则在它们之间添加一条边。
//对冲突图进行着色：
//使用最少的颜色为图着色，其中每种颜色对应一个寄存器。
//如果寄存器不足，则将部分变量溢出到内存。
//重写代码：
//为每个变量分配寄存器或栈位置。
//修改指令使变量的访问对应实际分配的位置。


//我来详细分析函数的活动记录和寄存器分配优化的实现。
//
//一、函数活动记录分析
//从代码可以看出，MipsBuilder中的函数活动记录包含以下分区：
//
//1. 参数区：
//- 前3个参数使用寄存器 a1-a3 传递
//- 超过3个的参数通过栈传递，从 sp+currentStackOffset+活跃寄存器数*4+4 开始向上存放
//
//2. 返回地址区：
//- ra 寄存器保存的返回地址存放在 sp+currentStackOffset+活跃寄存器数*4 处
//
//3. 寄存器保存区：
//- 保存活跃的寄存器(t0-t9, s0-s7等)
//- 从 sp+currentStackOffset 开始向上依次保存
//- 每个寄存器占4字节
//
//4. 局部变量区：
//- 从 sp+currentStackOffset 开始向下分配
//- 每个变量占4字节
//- 包括:
//  - alloca指令分配的局部变量
//  - 寄存器溢出的变量
//  - 临时值
//
//二、寄存器分配优化实现 - 线性扫描算法
//
//从代码中我选择分析线性扫描算法的实现。代码实现了基于活跃变量分析的线性扫描寄存器分配，主要特点如下：
//
//1. 寄存器池管理：
//private final Set<Register> availableRegisters; // 可用寄存器集合
//// 包含t0-t9(10个临时寄存器)和s0-s7(8个保存寄存器)
//
//2. 使用计数和优先级：
//// 变量使用频率计算
//useCount = 基础次数 + 10000 * 循环深度
//
//3. 分配策略：
//private void allocateRegister(Value value, BasicBlock block) {
//    // 1. 尝试分配空闲寄存器
//    for (Register reg : availableRegisters) {
//        if (!regToVarMap.containsKey(reg)) {
//            assignRegister(value, reg);
//            return;
//        }
//    }
//
//    // 2. 无空闲寄存器时进行溢出
//    Register regToSpill = selectRegisterToSpill(block);
//    if (regToSpill != null) {
//        Value spilledVar = regToVarMap.get(regToSpill);
//        varToRegMap.remove(spilledVar);
//        assignRegister(value, regToSpill);
//    }
//}
//
//4. 溢出选择策略：
//private Register selectRegisterToSpill(BasicBlock currentBlock) {
//    // 1. 优先选择不在Out集合中的变量
//    // 2. 在相同条件下选择使用频率最低的变量
//    // 3. 考虑循环嵌套深度的影响
//}
//
//主要优化效果：
//1. 通过活跃变量分析减少寄存器保存/恢复次数
//2. 优先为循环中频繁使用的变量分配寄存器
//3. 通过跨基本块的寄存器分配提高寄存器利用率
//4. 对函数调用处的寄存器使用进行优化
//
//这种基于线性扫描的实现相比图着色算法实现更简单，运行速度更快，虽然可能不如图着色算法得到的分配方案优化，但在实践中已经能取得不错的优化效果。