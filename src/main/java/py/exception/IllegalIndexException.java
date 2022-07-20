package py.exception;

public class IllegalIndexException extends Exception {
    private static final long serialVersionUID = 361252852879289696L;

    public IllegalIndexException() {
    }

    public IllegalIndexException(String message) {
        super(message);
    }

    public IllegalIndexException(Throwable cause) {
        super(cause);
    }

    public IllegalIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalIndexException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
