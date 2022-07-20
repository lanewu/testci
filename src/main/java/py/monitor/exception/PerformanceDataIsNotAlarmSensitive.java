package py.monitor.exception;

public class PerformanceDataIsNotAlarmSensitive extends Exception {

    private static final long serialVersionUID = 1L;

    public PerformanceDataIsNotAlarmSensitive() {
        super();
    }

    public PerformanceDataIsNotAlarmSensitive(String message) {
        super(message);
    }

    public PerformanceDataIsNotAlarmSensitive(String message, Throwable cause) {
        super(message, cause);
    }

    public PerformanceDataIsNotAlarmSensitive(Throwable cause) {
        super(cause);
    }
}