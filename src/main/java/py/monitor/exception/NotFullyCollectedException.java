package py.monitor.exception;

public class NotFullyCollectedException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotFullyCollectedException() {
        super();
    }

    public NotFullyCollectedException(String message) {
        super(message);
    }

    public NotFullyCollectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFullyCollectedException(Throwable cause) {
        super(cause);
    }

}
