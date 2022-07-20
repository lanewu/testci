package py.monitor.exception;

public class EmptyStoreException extends Exception {

    private static final long serialVersionUID = 1L;

    public EmptyStoreException() {
        super();
    }

    public EmptyStoreException(String message) {
        super(message);
    }

    public EmptyStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyStoreException(Throwable cause) {
        super(cause);
    }
}