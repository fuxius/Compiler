package ast;

import java.util.List;

/**
 * 编译单元节点
 * 对应文法：CompUnit → {Decl} {FuncDef} MainFuncDef
 */
public class CompUnitNode {
    // 声明列表
    private List<DeclNode> declNodes;
    // 函数定义列表
    private List<FuncDefNode> funcDefNodes;
    // 主函数定义
    private MainFuncDefNode mainFuncDefNode;

    public CompUnitNode(List<DeclNode> declNodes, List<FuncDefNode> funcDefNodes, MainFuncDefNode mainFuncDefNode) {
        this.declNodes = declNodes;
        this.funcDefNodes = funcDefNodes;
        this.mainFuncDefNode = mainFuncDefNode;
    }

    // 打印方法，用于输出语法分析结果
    public void print() {
        for (DeclNode declNode : declNodes) {
            declNode.print();
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            funcDefNode.print();
        }
        mainFuncDefNode.print();
        System.out.println("<CompUnit>");
    }
}
