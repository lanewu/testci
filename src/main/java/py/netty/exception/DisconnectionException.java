package py.netty.exception;

public class DisconnectionException extends Exception {
    private static final long serialVersionUID = -6826356559715731605L;

    public DisconnectionException() {
        super();
    }

    public DisconnectionException(String msg) {
        super(msg);
    }
}
