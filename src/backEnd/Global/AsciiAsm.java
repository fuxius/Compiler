package backEnd.Global;

public class AsciiAsm extends GlobalAsm {
    private String ascii;

    public AsciiAsm(String label, String ascii) {
        super(label);
        this.ascii = ascii;
    }

    @Override
    public String toString() {
        return label + ": .asciiz \"" + ascii + "\"";
    }
}
