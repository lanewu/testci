package py.monitor.pojo.management.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.DynamicMBean;
import javax.management.MBeanRegistration;
import javax.management.NotificationBroadcasterSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import py.common.struct.EndPoint;
import py.metrics.PYMetricRegistry.PYMetricNameBuilder;
import py.monitor.alarmbak.AlarmMessageData;
import py.monitor.customizable.repository.AttributeMetadata;
import py.monitor.dtd.Field;
import py.monitor.dtd.FieldID;
import py.monitor.jmx.repoter.notification.AlarmNotification;
import py.monitor.jmx.repoter.notification.PerformanceNotification;
import py.monitor.jmx.server.ResourceRelatedAttribute;

/**
 * we use this class to send the notification of the MBean to the {@code JmxClientListener}. and, the
 * {@code JmxClientListener} will dispatch the notification data to the "message pump". then the message-pump dispatch
 * the message to the {@code IMessageHandler} who will deal with the message
 * 
 * the notification data, who was define as:
 * 
 * <code>
 * public abstract class NotificableDynamicMBean extends ...{
 *     ...
 *     protected List<AttributeMetadata>; attributeMetadatas;
 *     ...
 *     public abstract void prepareNotifyData() throws Exception;
 *     ...
 * }
 * </code>
 * 
 * will be prepared in the derived class.
 * 
 * @author sxl
 * 
 */
public abstract class NotificableDynamicMBean extends NotificationBroadcasterSupport
        implements DynamicMBean, MBeanRegistration {
    private static final Logger logger = LoggerFactory.getLogger(NotificableDynamicMBean.class);
    protected AtomicLong sequenceNumber = new AtomicLong(1);
    protected List<ResourceRelatedAttribute> resourceRelatedAttributes;
    protected EndPoint endpoint;
    protected long parentTaskId;

    public EndPoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndPoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * send performance notification to {@code JmxClient}
     * 
     * TODO: should I send the attribute type here or just send the data, and convert the data in the client?? I chose
     * to just send the data, because it will cost a little more network resource
     * 
     * @throws Exception
     */
    public void sendPerformanceNotify() throws Exception {
        // get all monited attribute names
        List<Field> notificationDatas = new ArrayList<Field>();
        try {
            logger.debug("resourceRelatedAttribute are : {}", resourceRelatedAttributes);
            for (ResourceRelatedAttribute resourceRelatedAttribute : resourceRelatedAttributes) {
                logger.debug("{}", resourceRelatedAttribute);

                AttributeMetadata attributeMetadata = resourceRelatedAttribute.getAttributeMetadata();
                try {
                    Field data = new Field();
                    data.setId(new FieldID(parentTaskId, attributeMetadata.getId(), UUID.randomUUID()));
                    data.setName(attributeMetadata.getAttributeName());
                    data.setType(attributeMetadata.getAttributeType());

                    String resourceRelatedName = new PYMetricNameBuilder(attributeMetadata.getMbeanObjectName())
                            .type(attributeMetadata.getResourceType())
                            .id(String.valueOf(resourceRelatedAttribute.getResourceId())).build() + "."
                            + attributeMetadata.getAttributeName();
                    logger.debug("resource id: {}, resource related name: {}",
                            String.valueOf(resourceRelatedAttribute.getResourceId()), resourceRelatedName);
                    data.setData(SerializationUtils.serialize(this.getAttribute(resourceRelatedName)));
                    data.setDataSourceProvider(attributeMetadata.getMbeanObjectName());
                    data.setTimeStamp(System.currentTimeMillis());

                    // get resource type & id from MBean object name.
                    data.setResourceType(attributeMetadata.getResourceType());
                    data.setResourceId(resourceRelatedAttribute.getResourceId());

                    notificationDatas.add(data);
                } catch (Exception e) {
                    logger.warn("Caught an exception", e);
                }
            }

            logger.debug("Going to send performance data to jmx client, Sender:{},the data is {}",
                    this.getClass().toString(), notificationDatas.size());
            PerformanceNotification notification = new PerformanceNotification(this, sequenceNumber.getAndIncrement(),
                    System.currentTimeMillis(), PerformanceNotification.LIST_ATTRIBUTE_FOR_PERFORMANCE,
                    notificationDatas);
            logger.debug("Notification data is : {}", notificationDatas);
            this.sendNotification(notification);

            logger.debug("[{}] sent a performance notification [{}]", this.getClass().getName(), notification);
        } catch (Exception e) {
            logger.error("Caught an exception", e);
            throw e;
        }
    }

    public void sendAlarmNotify(AlarmMessageData data) throws Exception {
        logger.debug("Going to send alarm data to monitorcenter");
        AlarmNotification notification = new AlarmNotification(this, UUID.randomUUID().getLeastSignificantBits(),
                System.currentTimeMillis(), AlarmNotification.LIST_ATTRIBUTE_FOR_ALARM, data);
        this.sendNotification(notification);
    }
}
