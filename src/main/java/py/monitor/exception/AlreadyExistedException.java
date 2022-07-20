package py.monitor.exception;

public class AlreadyExistedException extends Exception {
    private static final long serialVersionUID = 1L;

    public AlreadyExistedException() {
        super();
    }

    public AlreadyExistedException(String message) {
        super(message);
    }

    public AlreadyExistedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyExistedException(Throwable cause) {
        super(cause);
    }
}
