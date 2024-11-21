package symbol;

import java.util.List;
import LLVMIR.Global.Function;
/**
 * 函数符号类，包含函数的特有信息
 */
public class FunctionSymbol extends Symbol {
    private String returnType;               // 返回类型
    private List<VariableSymbol> parameters; // 参数列表
    private Function LLVMIR;

    public Function getLLVMIR() {
        return LLVMIR;
    }

    public void setLLVMIR(Function LLVMIR) {
        this.LLVMIR = LLVMIR;
    }

    public FunctionSymbol(String name, int scopeLevel, String typeName, String returnType, List<VariableSymbol> parameters) {
        super(name, scopeLevel, typeName);
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<VariableSymbol> getParameters() {
        return parameters;
    }
}
