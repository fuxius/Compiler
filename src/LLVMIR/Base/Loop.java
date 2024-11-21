package LLVMIR.Base;

import LLVMIR.Base.BasicBlock;

/**
 * 表示 LLVM IR 中的循环
 */
public class Loop {
    private final BasicBlock header; // 循环头
    private final BasicBlock body;   // 循环体
    private final BasicBlock exit;   // 循环退出块

    /**
     * 构造循环
     *
     * @param header 循环头
     * @param body   循环体
     * @param exit   循环退出块
     */
    public Loop(BasicBlock header, BasicBlock body, BasicBlock exit) {
        if (header == null || body == null || exit == null) {
            throw new IllegalArgumentException("Header, body, and exit blocks cannot be null");
        }
        this.header = header;
        this.body = body;
        this.exit = exit;
    }

    /**
     * 获取循环头
     *
     * @return 循环头基本块
     */
    public BasicBlock getHeader() {
        return header;
    }

    /**
     * 获取循环体
     *
     * @return 循环体基本块
     */
    public BasicBlock getBody() {
        return body;
    }

    /**
     * 获取循环退出块
     *
     * @return 循环退出块基本块
     */
    public BasicBlock getExit() {
        return exit;
    }
}
