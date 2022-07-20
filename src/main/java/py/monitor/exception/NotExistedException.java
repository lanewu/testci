package py.monitor.exception;

public class NotExistedException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotExistedException() {
        super();
    }

    public NotExistedException(String message) {
        super(message);
    }

    public NotExistedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotExistedException(Throwable cause) {
        super(cause);
    }
}
