package py.monitor.exception;

public class UnsupportedIdentifierTypeException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnsupportedIdentifierTypeException() {
        super();
    }

    public UnsupportedIdentifierTypeException(String message) {
        super(message);
    }

    public UnsupportedIdentifierTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedIdentifierTypeException(Throwable cause) {
        super(cause);
    }
}
