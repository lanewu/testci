package py.exception;

public class BufferOverflowException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BufferOverflowException() {
        super();
    }

    public BufferOverflowException(String message) {
        super(message);
    }

    public BufferOverflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferOverflowException(Throwable cause) {
        super(cause);
    }
}
