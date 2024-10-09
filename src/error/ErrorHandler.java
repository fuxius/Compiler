package error;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private static ErrorHandler instance = new ErrorHandler();
    private List<String> errors = new ArrayList<>();

    public static ErrorHandler getInstance() {
        return instance;
    }

    public void reportError(int line, ErrorType type) {
        errors.add(line + " " + type.getCode());
    }

    public List<String> getErrors() {
        return errors;
    }

    public void clear() {
        errors.clear();
    }
}
