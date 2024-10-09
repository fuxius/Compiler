package error;

public class Error {
    private int lineNumber;
    private String errorType;
    private String message;

    public Error(int lineNumber, String errorType, String message) {
        this.lineNumber = lineNumber;
        this.errorType = errorType;
        this.message = message;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return lineNumber + " " + errorType + " " + message;
    }
}
