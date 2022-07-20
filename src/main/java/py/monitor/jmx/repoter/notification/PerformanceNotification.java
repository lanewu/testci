package py.monitor.jmx.repoter.notification;

import java.util.List;

import py.monitor.dtd.Field;

public class PerformanceNotification extends javax.management.Notification {
    /**
     * 
     */
    private static final long serialVersionUID = 8995700378381417418L;

    /**
     * Notification type which indicates that the observed MBean attribute value has changed. <BR>
     * The value of this type string is "list all attributes for performance".
     */
    public static final String LIST_ATTRIBUTE_FOR_PERFORMANCE = "list all attributes for performance";

    private final List<Field> attributes;

    public PerformanceNotification(Object source, long sequenceNumber, long timeStamp, String message,
            List<Field> attributes) {
        super(LIST_ATTRIBUTE_FOR_PERFORMANCE, source, sequenceNumber, timeStamp, message);
        this.attributes = attributes;
        setUserData(this.attributes);
    }

    public PerformanceNotification(Object source, long sequenceNumber, List<Field> attributes) {
        super(LIST_ATTRIBUTE_FOR_PERFORMANCE, source, sequenceNumber);
        this.attributes = attributes;
        setUserData(this.attributes);
    }

    public PerformanceNotification(Object source, long sequenceNumber, String message, List<Field> attributes) {
        super(LIST_ATTRIBUTE_FOR_PERFORMANCE, source, sequenceNumber, message);
        this.attributes = attributes;
        setUserData(this.attributes);
    }

    public PerformanceNotification(Object source, long sequenceNumber, long timeStamp, List<Field> attributes) {
        super(LIST_ATTRIBUTE_FOR_PERFORMANCE, source, sequenceNumber, timeStamp);
        this.attributes = attributes;
        setUserData(this.attributes);
    }

    public List<Field> getAttributes() {
        return attributes;
    }

}
