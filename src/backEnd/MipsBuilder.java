package backEnd;

import LLVMIR.Base.Value;
import LLVMIR.Global.Function;
import backEnd.Global.GlobalAsm;

import java.util.ArrayList;
import java.util.HashMap;

public class MipsBuilder {
    // 单例模式
    private static MipsBuilder instance = new MipsBuilder();
    // 标识现在的函数
    private Function currentFunction = null;
    // 跟踪当前处理的栈偏移量
    private int currentStackOffset = 0;
    // 使用 HashMap 将每个 Value 映射到对应的栈偏移量
    private HashMap<Value, Integer> stackOffsetMap = new HashMap<>();
    // 是否自动添加
    private boolean autoAdd = true;
    // 是否处于 main 函数中
    private boolean inMain = false;
    // .data 部分，存放数据，使用 GlobalAsm 类型数组存放
    private ArrayList<GlobalAsm> data = new ArrayList<>();
    // .text 部分，存放指令，使用 GlobalAsm 类型数组存放
    private ArrayList<GlobalAsm> text = new ArrayList<>();

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

    public void setAutoAdd(boolean autoAdd) {
        this.autoAdd = autoAdd;
    }

    public boolean isAutoAdd() {
        return autoAdd;
    }

    public void setInMain(boolean inMain) {
        this.inMain = inMain;
    }

    public boolean isInMain() {
        return inMain;
    }

    public void addData(GlobalAsm asm) {
        data.add(asm);
    }

    public void addText(GlobalAsm asm) {
        text.add(asm);
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        for (GlobalAsm asm : data) {
            sb.append(asm.toString());
        }
        sb.append(".text\n");
        for (GlobalAsm asm : text) {
            sb.append(asm.toString());
        }
        return sb.toString();
    }
}