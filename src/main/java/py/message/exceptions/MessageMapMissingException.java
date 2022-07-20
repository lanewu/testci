package py.message.exceptions;

public class MessageMapMissingException extends Exception {
    private static final long serialVersionUID = 1L;

    public MessageMapMissingException() {
        super();
    }

    public MessageMapMissingException(String message) {
        super(message);
    }

    public MessageMapMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageMapMissingException(Throwable cause) {
        super(cause);
    }
}
