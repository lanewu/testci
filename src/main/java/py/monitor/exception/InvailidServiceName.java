package py.monitor.exception;

public class InvailidServiceName extends Exception {

    private static final long serialVersionUID = 1L;

    public InvailidServiceName() {
        super();
    }

    public InvailidServiceName(String message) {
        super(message);
    }

    public InvailidServiceName(String message, Throwable cause) {
        super(message, cause);
    }

    public InvailidServiceName(Throwable cause) {
        super(cause);
    }
}