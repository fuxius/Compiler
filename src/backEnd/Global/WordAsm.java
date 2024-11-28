package backEnd.Global;

import java.util.List;

public class WordAsm extends GlobalAsm {
    private List<Integer> words;

    public WordAsm(String label, List<Integer> words) {
        super(label);
        this.words = words;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": .word ");
        for (int i = 0; i < words.size(); i++) {
            sb.append(words.get(i));
            if (i != words.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
