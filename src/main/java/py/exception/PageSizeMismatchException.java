package py.exception;

public class PageSizeMismatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PageSizeMismatchException() {
		super();
	}

	public PageSizeMismatchException(String message) {
		super(message);
	}

	public PageSizeMismatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public PageSizeMismatchException(Throwable cause) {
		super(cause);
	}
}
