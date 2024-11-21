package LLVMIR;

import LLVMIR.LLVMType.LLVMType;

import java.util.ArrayList;

public class Initial {
    private LLVMType type;
    private ArrayList<Integer> values;

    public Initial(LLVMType type, ArrayList<Integer> values) {
        this.type = type;
        this.values = values;
    }
}
