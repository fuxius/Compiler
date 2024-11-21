package LLVMIR;
import LLVMIR.LLVMType.LLVMType;
import java.util.ArrayList;
//User 类继承自 Value 类，表示使用 Value 的对象。LLVM IR中的许多对象（如指令、基本块）都可以作为 User。每个 User 包含操作数的列表（operands），这些操作数就是值的实例。
public class User extends Value{
    protected ArrayList<Value> operands;
    public User(String name, LLVMType type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    public void addOperand(Value value){
        operands.add(value);
        if(value!= null)    value.addUser(this);
    }

    public void modifyValue(Value oldValue,Value newValue){
        while(true){
            int index = operands.indexOf(oldValue);
            if(index == -1)     break;
            operands.set(index,newValue);
            newValue.addUser(this);
        }
    }
    public void removeoperands(){
        for(Value value:operands){
            if(value != null){
                value.removeUser(this);
            }
        }
    }
    public void setoperandserands(Value value,int pos) {
        this.operands.set(pos,value);
    }

    public ArrayList<Value> getoperandserands() {
        return operands;
    }
}
