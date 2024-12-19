package LLVMIR.Base;

import LLVMIR.Global.GlobalVar;
import LLVMIR.Ins.Branch;
import LLVMIR.Ins.Phi;
import LLVMIR.Ins.Ret;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Global.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 表示 LLVM IR 中的基本块，每个基本块是一个指令的序列
 */
public class BasicBlock extends User {
    private final ArrayList<Instruction> instrs; // 基本块中的指令
    private final Function parentFunc;      // 所属的函数
    private ArrayList<BasicBlock> predecessors; // 前驱基本块
    private ArrayList<BasicBlock> successors; // 后继基本块
    // 支配者基本块列表
    private ArrayList<BasicBlock> dominators;
    // 被支配基本块列表
    private ArrayList<BasicBlock> dominatedBy;
    // 直接支配者
    private BasicBlock immediateDominator;
    // 直接支配的基本块列表
    private ArrayList<BasicBlock> immediateDominates;
    // 支配边界列表
    private ArrayList<BasicBlock> dominanceFrontier;
    // 直接支配树的深度
    private int imdomDepth;
    // 活跃变量分析所需的属性
    private HashSet<Value> defSet;
    private HashSet<Value> useSet;
    private HashSet<Value> inSet;
    private HashSet<Value> outSet;

    // 是否已经被删除
    private boolean isDeleted = false;
    /**
     * 构造基本块
     *
     * @param name       基本块的名称
     * @param parentFunc 所属的函数
     */
    public BasicBlock(String name, Function parentFunc) {
        super(name, LLVMType.funcType);

        if (parentFunc == null) {
            throw new IllegalArgumentException("Parent function cannot be null");
        }

        this.instrs = new ArrayList<>();
        this.parentFunc = parentFunc;

        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.dominators = new ArrayList<>();
        this.dominatedBy = new ArrayList<>();
        this.immediateDominates = new ArrayList<>();
        this.dominanceFrontier = new ArrayList<>();
        this.imdomDepth = 0;
        this.defSet = new HashSet<>();
        this.useSet = new HashSet<>();
        this.inSet = new HashSet<>();
        this.outSet = new HashSet<>();
    }

    /**
     * 向基本块中添加指令
     *
     * @param instr 要添加的指令
     */
    public void addInstr(Instruction instr) {
        if (instr == null) {
            throw new IllegalArgumentException("Instruction cannot be null");
        }
        instrs.add(instr);
    }

    /**
     * 检查基本块是否以返回指令结束
     *
     * @return 如果最后一条指令是返回指令，返回 true
     */
    public boolean hasRet() {
        return !instrs.isEmpty() &&
                instrs.get(instrs.size() - 1).getInstrType() == Instruction.InstrType.RETURN;
    }

    /**
     * 获取基本块中的所有指令
     *
     * @return 指令列表
     */
    public List<Instruction> getInstrs() {
        return instrs;
    }

    /**
     * 获取所属的函数
     *
     * @return 所属的函数
     */
    public Function getParentFunc() {
        return parentFunc;
    }

    /**
     * 返回 LLVM IR 格式的基本块字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Name).append(":\n");
        for (Instruction instr : instrs) {
            sb.append("\t").append(instr).append("\n");
        }
        return sb.toString();
    }
    // 判断是否已经有跳转指令
    public boolean hasBr() {
        if (instrs.isEmpty()) return false;

        // 获取最后一条指令，判断是否是跳转指令
        Instruction lastInstr = instrs.get(instrs.size() - 1);
        return lastInstr instanceof Branch ;
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public void setPredecessors(ArrayList<BasicBlock> predecessors) {
        this.predecessors = predecessors;
    }

    public ArrayList<BasicBlock> getSuccessors() {
        return successors;
    }

    public void setSuccessors(ArrayList<BasicBlock> successors) {
        this.successors = successors;
    }
    public void deleteForPhi(BasicBlock block){
        for(User user:users){
            if(user instanceof Phi phi && phi.getParentBlock()==block){
                for(int i=0;i<=phi.getIncomingBlocks().size()-1;i++){
                    if(phi.getIncomingBlocks().get(i)==this){
                        phi.getIncomingBlocks().remove(i);
                        phi.getOperands().remove(i);
                        i--;
                    }
                }
            }
        }
    }

    // 获取最后一条指令
    public Instruction getLastInstr() {
        if (instrs.isEmpty()) return null;
        return instrs.get(instrs.size() - 1);
    }

    // 添加前驱和后继
    public void addPredecessor(BasicBlock pred) {
        predecessors.add(pred);
    }

    public void addSuccessor(BasicBlock succ) {
        successors.add(succ);
    }


    public void setDeleted() {
        isDeleted = true;
    }

    public boolean isDeleted() {
        return isDeleted;
    }


    public HashSet<Value> getDefSet() {
        return defSet;
    }

    public HashSet<Value> getUseSet() {
        return useSet;
    }

    public HashSet<Value> getInSet() {
        return inSet;
    }

    public void setInSet(HashSet<Value> inSet) {
        this.inSet = inSet;
    }

    public HashSet<Value> getOutSet() {
        return outSet;
    }

    public void setOutSet(HashSet<Value> outSet) {
        this.outSet = outSet;
    }

    public void setDefSet(HashSet<Value> defSet) {
        this.defSet = defSet;
    }

    public void setUseSet(HashSet<Value> useSet) {
        this.useSet = useSet;
    }

    // 计算Def和Use集合
    public void computeDefUse() {
        HashSet<Value> def = new HashSet<>();
        HashSet<Value> use = new HashSet<>();
        // 遍历基本块中的指令
        for(Instruction instr:instrs){
            if(instr instanceof Phi){
                for(Value value:instr.getOperands()){
                    if(value instanceof Instruction || value instanceof Param || value instanceof GlobalVar){
                        use.add(value);
                    }
                }
            }
        }
        for (Instruction instr : instrs) {
            // 遍历指令的操作数
            for (Value value : instr.getOperands()) {
                // 如果操作数不在 Def 集合中，且是指令、参数或全局变量，则加入 Use 集合
                if (!def.contains(value) && (value instanceof Instruction || value instanceof Param || value instanceof GlobalVar)) {
                    use.add(value);
                }
            }
            // 如果指令有左值，且不在 Use 集合中，则加入 Def 集合
            if (!use.contains(instr) && instr.hasLVal()) {
                def.add(instr);
            }
        }
        this.useSet = use;
        this.defSet = def;
    }
    // 获取支配者基本块列表
    public ArrayList<BasicBlock> getDominators() {
        return dominators;
    }

    // 设置支配者基本块列表
    public void setDom(ArrayList<BasicBlock> dominators) {
        this.dominators = dominators;
    }

    // 获取被支配基本块列表
    public ArrayList<BasicBlock> getDominatedBy() {
        return dominatedBy;
    }

    // 设置被支配基本块列表
    public void setDominatedBy(ArrayList<BasicBlock> dominatedBy) {
        this.dominatedBy = dominatedBy;
    }

    // 获取直接支配者
    public BasicBlock getImmediateDominator() {
        return immediateDominator;
    }

    // 设置直接支配者
    public void setImdommedBy(BasicBlock immediateDominator) {
        this.immediateDominator = immediateDominator;
    }

    // 获取直接支配的基本块列表
    public ArrayList<BasicBlock> getImdom() {
        return immediateDominates;
    }

    // 设置直接支配的基本块列表
    public void setImdom(ArrayList<BasicBlock> immediateDominates) {
        this.immediateDominates = immediateDominates;
    }

    // 获取支配边界列表
    public ArrayList<BasicBlock> getDF() {
        return dominanceFrontier;
    }

    // 设置支配边界列表
    public void setDF(ArrayList<BasicBlock> dominanceFrontier) {
        this.dominanceFrontier = dominanceFrontier;
    }

    // 获取直接支配树的深度
    public int getImdomDepth() {
        return imdomDepth;
    }

    // 设置直接支配树的深度
    public void setImdomDepth(int imdomDepth) {
        this.imdomDepth = imdomDepth;
    }

}
