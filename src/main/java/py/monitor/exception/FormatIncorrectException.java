package py.monitor.exception;

public class FormatIncorrectException extends Exception {
    private static final long serialVersionUID = 1L;

    public FormatIncorrectException() {
        super();
    }

    public FormatIncorrectException(String message) {
        super(message);
    }

    public FormatIncorrectException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatIncorrectException(Throwable cause) {
        super(cause);
    }
}
