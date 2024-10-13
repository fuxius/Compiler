package ast;

import java.util.List;

/**
 * 函数形参表节点
 * 对应文法：FuncFParams → FuncFParam { ',' FuncFParam }
 */
public class FuncFParamsNode {
    private List<FuncFParamNode> funcFParamNodes;

    public FuncFParamsNode(List<FuncFParamNode> funcFParamNodes) {
        this.funcFParamNodes = funcFParamNodes;
    }

    public void print() {
        for (int i = 0; i < funcFParamNodes.size(); i++) {
            funcFParamNodes.get(i).print();
            if (i < funcFParamNodes.size() - 1) {
                System.out.println("COMMA ,");
            }
        }
        // 不需要输出 <FuncFParams>，根据您的要求
    }
}
