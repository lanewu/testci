package py.monitor.jmx.repoter.notification;

import py.monitor.alarmbak.AlarmMessageData;

public class AlarmNotification extends javax.management.Notification {
    private static final long serialVersionUID = -4086649735309058350L;

    public static final String LIST_ATTRIBUTE_FOR_ALARM = "list all attributes for alarm";

    private final AlarmMessageData notificationData;

    public AlarmNotification(Object source, long sequenceNumber, long timeStamp, AlarmMessageData data) {
        super(LIST_ATTRIBUTE_FOR_ALARM, source, sequenceNumber, timeStamp);
        this.notificationData = data;
        setUserData(this.notificationData);
    }

    public AlarmNotification(Object source, long sequenceNumber, long timeStamp, String message, AlarmMessageData data) {
        super(LIST_ATTRIBUTE_FOR_ALARM, source, sequenceNumber, timeStamp, message);
        this.notificationData = data;
        setUserData(this.notificationData);
    }

    public AlarmNotification(Object source, long sequenceNumber, AlarmMessageData data) {
        super(LIST_ATTRIBUTE_FOR_ALARM, source, sequenceNumber);
        this.notificationData = data;
        setUserData(this.notificationData);
    }

    public AlarmNotification(Object source, long sequenceNumber, String message, AlarmMessageData data) {
        super(LIST_ATTRIBUTE_FOR_ALARM, source, sequenceNumber, message);
        this.notificationData = data;
        setUserData(this.notificationData);
    }

    public AlarmMessageData getNotificationData() {
        return notificationData;
    }
}
