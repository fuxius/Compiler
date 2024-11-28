package backEnd.Global;

import java.util.List;

public class ByteAsm extends GlobalAsm {
    private List<Integer> bytes;
    private int count;

    // 用于初始化为零的情况
    public ByteAsm(String label, int count) {
        super(label);
        this.count = count;
    }

    // 用于指定字节值的情况
    public ByteAsm(String label, List<Integer> bytes) {
        super(label);
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": .byte ");
        if (bytes != null) {
            for (int i = 0; i < bytes.size(); i++) {
                sb.append(bytes.get(i));
                if (i != bytes.size() - 1) {
                    sb.append(", ");
                }
            }
        } else {
            sb.append("0:").append(count);
        }
        return sb.toString();
    }
}
