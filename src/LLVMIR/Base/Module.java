package LLVMIR.Base;

import LLVMIR.Global.ConstStr;
import LLVMIR.Global.GlobalVar;
import LLVMIR.Global.Function;
import LLVMIR.LLVMType.LLVMType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 表示 LLVM IR 中的模块
 * 包含全局变量、函数和常量字符串的集合
 */
public class Module extends Value {
    private final List<GlobalVar> globalVars;  // 全局变量
    private final List<Function> functions;   // 函数
    private final List<ConstStr> constStrs;   // 常量字符串

    /**
     * 构造模块对象
     */
    public Module() {
        super("module", LLVMType.funcType); // 使用非 null 的类型，例如 funcType
        this.globalVars = new ArrayList<>();
        this.functions = new ArrayList<>();
        this.constStrs = new ArrayList<>();
    }
    /**
     * 添加全局变量
     *
     * @param globalVar 全局变量对象
     */
    public void addGlobalVar(GlobalVar globalVar) {
        if (globalVar == null) {
            throw new IllegalArgumentException("Global variable cannot be null");
        }
        globalVars.add(globalVar);
    }

    /**
     * 添加函数
     *
     * @param function 函数对象
     */
    public void addFunction(Function function) {
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null");
        }
        functions.add(function);
    }

    /**
     * 添加常量字符串
     *
     * @param constant 常量字符串对象
     */
    public void addConstant(ConstStr constant) {
        if (constant == null) {
            throw new IllegalArgumentException("Constant string cannot be null");
        }
        constStrs.add(constant);
    }

    /**
     * 获取全局变量列表
     *
     * @return 全局变量列表
     */
    public List<GlobalVar> getGlobalVars() {
        return globalVars;
    }

    /**
     * 获取函数列表
     *
     * @return 函数列表
     */
    public List<Function> getFunctions() {
        return functions;
    }

    /**
     * 获取常量字符串列表
     *
     * @return 常量字符串列表
     */
    public List<ConstStr> getConstStrs() {
        return constStrs;
    }

    /**
     * 返回模块的 LLVM IR 表示字符串
     *
     * @return 格式化的字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare i32 @getint() \n")
                .append("declare i32 @getchar() \n")
                .append("declare void @putint(i32)\n")
                .append("declare void @putch(i32)\n")
                .append("declare void @putstr(i8* )\n");

        // 处理常量字符串
        for (ConstStr cstStr : constStrs) {
            sb.append(cstStr).append("\n");
        }

        // 处理全局变量
        for (GlobalVar globalVar : globalVars) {
            sb.append(globalVar).append("\n");
        }

        // 处理函数
        for (int i = 0; i < functions.size(); i++) {
            sb.append(functions.get(i));
            if (i < functions.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 将模块的 LLVM IR 写入文件
     *
     * @param filename 文件名
     */
    public void writeToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(this.toString());
        } catch (IOException e) {
            System.err.println("Error writing LLVM IR to file (" + filename + "): " + e.getMessage());
        }
    }
}
