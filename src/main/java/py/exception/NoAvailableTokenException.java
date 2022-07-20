package py.exception;

public class NoAvailableTokenException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 361252852879289696L;

    public NoAvailableTokenException() {

    }

    public NoAvailableTokenException(String message) {
        super(message);

    }

    public NoAvailableTokenException(Throwable cause) {
        super(cause);
    }

    public NoAvailableTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoAvailableTokenException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
