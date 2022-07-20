package py.exception;

public class SegmentSizeMismatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SegmentSizeMismatchException() {
		super();
	}

	public SegmentSizeMismatchException(String message) {
		super(message);
	}

	public SegmentSizeMismatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public SegmentSizeMismatchException(Throwable cause) {
		super(cause);
	}
}
