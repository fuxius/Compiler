package backEnd.Instruction.Br;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;
import backEnd.Instruction.Sys.Mem;

import java.util.ArrayList;

public class JumpAsm extends AsmInstruction {
    public enum JumpOp {
        j, jal, jr, jalr
    }
    private JumpOp op;
    private String label;
    private Register rs;
    private int imm;
    //Load Word 指令列表
    private ArrayList<Mem> loadAsms;
    //Store Word 指令列表
    private ArrayList<Mem> storeAsms;

    // j, jal
    public JumpAsm(JumpOp op, String label) {
        this.op = op;
        this.label = label;
    }
    // jr, jalr
    public JumpAsm(JumpOp op, Register rs) {
        this.op = op;
        this.rs = rs;
    }

    public JumpOp getOp() {
        return op;
    }

    public String getLabel() {
        return label;
    }

    public Register getRs() {
        return rs;
    }

    public int getImm() {
        return imm;
    }

    public ArrayList<Mem> getLoadAsms() {
        return loadAsms;
    }

    public void setLoadAsms(ArrayList<Mem> loadAsms) {
        this.loadAsms = loadAsms;
    }

    public ArrayList<Mem> getStoreAsms() {
        return storeAsms;
    }

    public void setStoreAsms(ArrayList<Mem> storeAsms) {
        this.storeAsms = storeAsms;
    }

    @Override
    public String toString() {
        if (op == JumpOp.jr || op == JumpOp.jalr) {
            return op + " " + rs;
        } else {
            return op + " " + label;
        }
    }

}
