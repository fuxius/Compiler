package LLVMIR.Ins;

import LLVMIR.Base.BasicBlock;
import LLVMIR.Base.Instruction;
import LLVMIR.LLVMType.LLVMType;
import LLVMIR.Base.Value;

import java.util.ArrayList;

public class Phi extends Instruction {
    private ArrayList<BasicBlock> incomingBlocks;

    public Phi(String name, BasicBlock parentBlock, ArrayList<BasicBlock> incomingBlocks, LLVMType type) {
        super(name, type, InstrType.PHI, parentBlock);
        this.incomingBlocks = incomingBlocks;
        // 确保 operands 被初始化
        if (this.operands == null) {
            this.operands = new ArrayList<>();
        }
        for (int i = 0; i < incomingBlocks.size(); i++) {
            operands.add(null);
            incomingBlocks.get(i).addUser(this);
        }
    }

    public void addIncomingValue(BasicBlock block, Value value) {
        int index = incomingBlocks.indexOf(block);
        operands.set(index, value);
        value.addUser(this);

    }

    @Override
    public void modifyValue(Value oldValue, Value newValue) {
        if (oldValue instanceof BasicBlock) {
            while (true) {
                int index = incomingBlocks.indexOf(oldValue);
                if (index == -1) break;
                incomingBlocks.set(index, (BasicBlock) newValue);
                newValue.addUser(this);
            }
        } else {
            while (true) {
                int index = operands.indexOf(oldValue);
                if (index == -1) break;
                operands.set(index, newValue);
                newValue.addUser(this);
            }
        }
    }

    public ArrayList<BasicBlock> getIncomingBlocks() {
        return incomingBlocks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Name + " = phi " + type + " ");
        for (int i = 0; i < incomingBlocks.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("[ ");
            // 添加 null 检查
            if (operands.get(i) != null) {
                sb.append(operands.get(i).getName());
            } else {
                sb.append("<null>");
            }
            sb.append(", %").append(incomingBlocks.get(i).getName()).append(" ]");
        }
        return sb.toString();
    }
}
