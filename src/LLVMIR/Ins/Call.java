package LLVMIR.Ins;

import LLVMIR.BasicBlock;
import LLVMIR.Instruction;
import LLVMIR.Value;
import LLVMIR.Global.Function;
import java.util.ArrayList;

public class Call extends Instruction {
    public Call(Function func, String name, ArrayList<Value> values, BasicBlock parent){
        super(name, func.getReturnType(),InstrType.CALL,parent);
        addOperand(func);
        for(Value value:values){
            addOperand(value);
        }
    }



    public String getGvnHash(){
        StringBuilder sb=new StringBuilder(operands.get(0).getName());
        sb.append("(");
        for(int i=1;i<=operands.size()-1;i++){
            if(i==1){
                sb.append(operands.get(i).getName());
            }
            else{
                sb.append(",").append(operands.get(i).getName());
            }
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder();
        for(int i=1;i<=operands.size()-1;i++){
            if(i>1){
                sb.append(",");
            }
            sb.append(operands.get(i).getType()).append(" ").append(operands.get(i).getName());
        }
        if (type.isVoid()) {
            return "call void " + operands.get(0).getName() + "(" + sb.toString() +")";
        } else {
            return Name + " = call i32 " + operands.get(0).getName() + "(" + sb.toString() +")";
        }
    }
}
