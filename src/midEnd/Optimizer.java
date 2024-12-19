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
