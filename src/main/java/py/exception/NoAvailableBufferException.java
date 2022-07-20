package py.exception;

public class NoAvailableBufferException extends Exception {

    private static final long serialVersionUID = 7255940931309136266L;

    public NoAvailableBufferException() {
        
    }

    public NoAvailableBufferException(String message) {
        super(message);
        
    }

    public NoAvailableBufferException(Throwable cause) {
        super(cause);
    }

    public NoAvailableBufferException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoAvailableBufferException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
