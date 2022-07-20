package py.monitor.exception;

public class NotAlarmSensitiveException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotAlarmSensitiveException() {
        super();
    }

    public NotAlarmSensitiveException(String message) {
        super(message);
    }

    public NotAlarmSensitiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAlarmSensitiveException(Throwable cause) {
        super(cause);
    }
}
