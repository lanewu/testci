package py.exception;

public class NotSupportedException extends RuntimeException {
    private static final long serialVersionUID = -2314156399478356586L;

    public NotSupportedException() {
    }

    public NotSupportedException(String message) {
        super(message);
    }

    public NotSupportedException(Throwable cause) {
        super(cause);
    }

    public NotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
