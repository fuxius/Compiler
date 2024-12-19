package midEnd;
import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;
import midEnd.base.ActiveVarAnalyzer;
import midEnd.base.CFGBuilder;
import midEnd.base.RegAlloc;
import midEnd.helper.delete;
import midEnd.mem.MemToReg;
import midEnd.mem.Remove;

public class Optimizer {
    // 单例模式
    private static Optimizer instance = new Optimizer();

    private Optimizer() {
    }

    public static Optimizer getInstance() {
        return instance;
    }
    private boolean basicOptimize = true;

    public boolean isBasicOptimize() {
        return basicOptimize;
    }

    public void run(Module module) {
//         调用delete的simplify方法
        delete.simplify(module);
        // 调用CFGBuilder的run方法
        CFGBuilder.buildCFG(module);
//        // 调用MemToReg的execute方法
        MemToReg.execute(module);
        // 再次调用CFGBuilder的run方法
        CFGBuilder.buildCFG(module);
        // 调用ActiveVarAnalyzer的analyze方法
        for (Function func : module.getFunctions()) {
            ActiveVarAnalyzer.analyze(func);
        }

        // 调用 RegAlloc
        RegAlloc regAlloc = new RegAlloc();
        regAlloc.allocateRegisters(module);
        Remove.removePhi(module);
        delete.simplify(module);
    }
}
