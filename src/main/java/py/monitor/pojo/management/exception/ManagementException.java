package py.monitor.pojo.management.exception;
/**
 * 
 * @author sxl
 *
 */
public class ManagementException extends Exception {
    private static final long serialVersionUID = 1L;

    public ManagementException() {
    }

    public ManagementException(String message) {
        super(message);
    }

    public ManagementException(Throwable cause) {
        super(cause);
    }

    public ManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
