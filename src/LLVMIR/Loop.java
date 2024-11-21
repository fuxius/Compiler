package LLVMIR;

public class Loop {
    private BasicBlock header;     // 循环头
    private BasicBlock body;       // 循环体
    private BasicBlock exit;       // 循环退出块

    public Loop(BasicBlock header, BasicBlock body, BasicBlock exit) {
        this.header = header;
        this.body = body;
        this.exit = exit;
    }

    public BasicBlock getHeader() {
        return header;
    }

    public void setHeader(BasicBlock header) {
        this.header = header;
    }

    public BasicBlock getBody() {
        return body;
    }

    public void setBody(BasicBlock body) {
        this.body = body;
    }

    public BasicBlock getExit() {
        return exit;
    }

    public void setExit(BasicBlock exit) {
        this.exit = exit;
    }
}