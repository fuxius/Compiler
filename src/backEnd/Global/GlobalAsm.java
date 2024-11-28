package backEnd.Global;

import backEnd.Base.AsmInstruction;
import backEnd.Base.Register;

import java.util.List;

public abstract class GlobalAsm extends AsmInstruction {
    protected String label;

    public GlobalAsm(String label) {
        this.label = label;
    }

    @Override
    public abstract String toString();
}

