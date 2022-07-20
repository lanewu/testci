package py.monitor.jmx.repoter.notification;

public class TestingNotification extends javax.management.Notification {
    /* Serial version */
    private static final long serialVersionUID = 5385460073005138578L;

    /**
     * Notification type which indicates that the observed MBean attribute value has changed. <BR>
     * The value of this type string is <CODE>jmx.attribute.change</CODE>.
     */
    public static final String ATTRIBUTE_CHANGE = "jmx.attribute.change";

    /**
     * @serial The MBean attribute name.
     */
    private String attributeName = null;

    /**
     * @serial The MBean attribute type.
     */
    private String attributeType = null;

    /**
     * @serial The MBean attribute old value.
     */
    private Object oldValue = null;

    /**
     * @serial The MBean attribute new value.
     */
    private Object newValue = null;

    /**
     * Constructs an attribute change notification object.
     * In addition to the information common to all notification, the caller must supply the name and type
     * of the attribute, as well as its old and new values.
     * 
     * @param source
     *            The notification producer, that is, the MBean the attribute belongs to.
     * @param sequenceNumber
     *            The notification sequence number within the source object.
     * @param timeStamp
     *            The date at which the notification is being sent.
     * @param msg
     *            A String containing the message of the notification.
     * @param attributeName
     *            A String giving the name of the attribute.
     * @param attributeType
     *            A String containing the type of the attribute.
     * @param oldValue
     *            An object representing value of the attribute before the change.
     * @param newValue
     *            An object representing value of the attribute after the change.
     */
    public TestingNotification(Object source, long sequenceNumber, long timeStamp, String msg, String attributeName,
            String attributeType, Object oldValue, Object newValue) {

        super(TestingNotification.ATTRIBUTE_CHANGE, source, sequenceNumber, timeStamp, msg);
        this.attributeName = attributeName;
        this.attributeType = attributeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the name of the attribute which has changed.
     * 
     * @return A String containing the name of the attribute.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Gets the type of the attribute which has changed.
     * 
     * @return A String containing the type of the attribute.
     */
    public String getAttributeType() {
        return attributeType;
    }

    /**
     * Gets the old value of the attribute which has changed.
     * 
     * @return An Object containing the old value of the attribute.
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new value of the attribute which has changed.
     * 
     * @return An Object containing the new value of the attribute.
     */
    public Object getNewValue() {
        return newValue;
    }
}
