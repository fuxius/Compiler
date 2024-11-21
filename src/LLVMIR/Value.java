package LLVMIR;

import LLVMIR.LLVMType.LLVMType;

import javax.lang.model.element.Name;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;

//Value 类代表LLVM中的所有“值”，它是所有具体操作数（如变量、常量、指令结果等）的基类。每个 Value 都有一个名称（name）、类型（type）以及所有使用它的用户（users）。
public class Value {
    protected String Name;
    protected LLVMType type;
    protected List<User> users;;
    public Value(String name, LLVMType type){
        this.Name = name;
        this.type = type;
        this.users = new ArrayList<>();
    }
    public String getName() {
        return Name;
    }
    public void setName(String name) {
        Name = name;
    }
    public LLVMType getType() {
        return type;
    }
    public void setType(LLVMType type) {
        this.type = type;
    }
    public List<User> getusers() {
        return users;
    }
    public void setusers(ArrayList<User> users) {
        this.users = users;
    }
    public void addUser(User user){
        if(!users.contains(user)){
            users.add(user);
        }
    }
    public void removeUser(User user){
        if(users.contains(user)){
            users.remove(user);
        }
    }
    public void modifyValueForUsers(Value newValue){
        for(User user:users){
            user.modifyValue(this,newValue);
        }
        users=new ArrayList<>();
    }
}
