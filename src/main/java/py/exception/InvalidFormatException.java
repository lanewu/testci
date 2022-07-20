package py.exception;

public class InvalidFormatException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidFormatException() {
		super();
	}

	public InvalidFormatException(String message) {
		super(message);
	}

	public InvalidFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidFormatException(Throwable cause) {
		super(cause);
	}
}
