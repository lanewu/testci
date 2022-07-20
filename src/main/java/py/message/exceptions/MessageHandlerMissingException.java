package py.message.exceptions;

public class MessageHandlerMissingException extends Exception {
    private static final long serialVersionUID = 1L;

    public MessageHandlerMissingException() {
        super();
    }

    public MessageHandlerMissingException(String message) {
        super(message);
    }

    public MessageHandlerMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageHandlerMissingException(Throwable cause) {
        super(cause);
    }
}
