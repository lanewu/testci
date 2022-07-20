package py.common.tlsf;

/**
 * 
 * @author zjm
 *
 */
public class OutOfSpaceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OutOfSpaceException() {
        super();
    }

    public OutOfSpaceException(String message) {
        super(message);
    }

    public OutOfSpaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutOfSpaceException(Throwable cause) {
        super(cause);
    }
}
