package midEnd;
import LLVMIR.Base.Module;
import LLVMIR.Global.Function;

public class Optimizer {
    // 单例模式
    private static Optimizer instance = new Optimizer();

    private Optimizer() {
    }

    public static Optimizer getInstance() {
        return instance;
    }


    public void run(Module module) {
        // 调用CFGBuilder的run方法
        CFGBuilder.buildCFG(module);
        // 调用ActiveVarAnalyzer的analyze方法
        for (Function func : module.getFunctions()) {
            ActiveVarAnalyzer.analyze(func);
        }

        // 调用 RegAlloc
        RegAlloc regAlloc = new RegAlloc();
        regAlloc.allocateRegisters(module);


    }
}
