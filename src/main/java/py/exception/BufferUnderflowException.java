package py.exception;

public class BufferUnderflowException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BufferUnderflowException() {
        super();
    }

    public BufferUnderflowException(String message) {
        super(message);
    }

    public BufferUnderflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferUnderflowException(Throwable cause) {
        super(cause);
    }
}
