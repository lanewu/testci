package py.netty.exception;

public class TimeoutException extends Exception {
    private static final long serialVersionUID = 5643601435652588548L;

    public TimeoutException() {
        super();
    }

    public TimeoutException(String msg) {
        super(msg);
    }

}
