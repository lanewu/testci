package py.exception;

public class FilterDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    public FilterDeniedException() {
        super();
    }

    public FilterDeniedException(String message) {
        super(message);
    }

    public FilterDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilterDeniedException(Throwable cause) {
        super(cause);
    }
}