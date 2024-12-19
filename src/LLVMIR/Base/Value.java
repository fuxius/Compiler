package LLVMIR.Base;

import LLVMIR.LLVMType.LLVMType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示 LLVM IR 中的值 (Value)
 * 所有具体操作数（如变量、常量、指令结果等）的基类
 */
public class Value {
    protected String Name;                // 值的名称
    protected LLVMType type;              // 值的类型
    protected ArrayList<User> users;           // 使用该值的用户列表

    /**
     * 构造 Value 对象
     *
     * @param Name 值的名称
     * @param type 值的类型
     */
    public Value(String Name, LLVMType type) {
        this.Name = Name;
        this.type = type;
        this.users = new ArrayList<>();
    }

    /**
     * 获取值的名称
     *
     * @return 值名称
     */
    public String getName() {
        return Name;
    }
    //去掉第一位的Name
    public String getRealName() {
        return Name.substring(1);
    }
    /**
     * 设置值的名称
     *
     * @param Name 新的值名称
     */
    public void setName(String Name) {
        this.Name = Name;
    }

    /**
     * 获取值的类型
     *
     * @return 值类型
     */
    public LLVMType getType() {
        return type;
    }

    /**
     * 设置值的类型
     *
     * @param type 新的值类型
     */
    public void setType(LLVMType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        this.type = type;
    }

    /**
     * 获取用户列表
     *
     * @return 用户列表的只读视图
     */
    public ArrayList<User> getUsers() {
        return users;
    }

    /**
     * 设置用户列表
     *
     * @param users 新的用户列表
     */
    public void setUsers(List<User> users) {
        if (users == null) {
            throw new IllegalArgumentException("Users cannot be null");
        }
        this.users = new ArrayList<>(users); // 深拷贝
    }

    /**
     * 添加用户
     *
     * @param user 新用户
     */
    public void addUser(User user) {
        if (user != null && !users.contains(user)) {
            users.add(user);
        }
    }

    /**
     * 移除用
     *
     * @param user 要移除的用
     */
    public void removeUser(User user) {
        if(users.contains(user)){
            users.remove(user);
        }
    }

    /**
     * 修改所有用户的值引用
     *
     * @param newValue 替换的新值
     */
    public void modifyValueForUsers(Value newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("New value cannot be null");
        }
        for (User user : users) {
            user.modifyValue(this, newValue);
        }
        users.clear();
    }
}
