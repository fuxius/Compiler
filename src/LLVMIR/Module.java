package LLVMIR;

import LLVMIR.Global.ConstStr;
import LLVMIR.Global.GlobalVar;
import LLVMIR.Global.Function;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Module extends Value{
    private List<GlobalVar> globalVars;
    private List<Function> functions;
    private List<ConstStr> constStrs;

    public Module() {
        super(null,null);
        globalVars = new ArrayList<>();
        functions = new ArrayList<>();
        constStrs = new ArrayList<>();
    }

    public void addGlobalVar(GlobalVar globalVar) {
        globalVars.add(globalVar);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addConstant(ConstStr constant) {
        constStrs.add(constant);
    }

    // 获取全局变量、函数等

    public List<GlobalVar> getGlobalVars() {
        return globalVars;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public List<ConstStr> getConstStrs() {
        return constStrs;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare i32 @getint() \n" +
                "declare i32 @getchar() \n" +
                "declare void @putint(i32)\n" +
                "declare void @putch(i32)\n" +
                "declare void @putstr(i8* )\n");

        // 处理常量字符串
        for (ConstStr cstStr : constStrs) {
            if (cstStr != null) {
                sb.append(cstStr.toString());
                sb.append("\n");
            }
        }

        // 处理全局变量
        for (GlobalVar globalVar : globalVars) {
            if (globalVar != null) {
                sb.append(globalVar.toString());
                sb.append("\n");
            }
        }

        // 处理函数
        for (Function function : functions) {
            if (function != null) {
                sb.append(function.toString());
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public void writeToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // 调试：输出所有的 Function、GlobalVar 和 ConstStr
            System.out.println("Functions:");
            for (Function function : functions) {
                System.out.println(function != null ? function.getName() : "null");
            }

            System.out.println("GlobalVars:");
            for (GlobalVar globalVar : globalVars) {
                System.out.println(globalVar != null ? globalVar.getName() : "null");
            }

            System.out.println("ConstStrs:");
            for (ConstStr constStr : constStrs) {
                System.out.println(constStr != null ? constStr.getName() : "null");
            }

            // 输出 IR
            writer.write(this.toString());
        } catch (IOException e) {
            System.err.println("Error writing LLVM IR to file: " + e.getMessage());
        }
    }

}

