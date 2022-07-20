package py.monitor.customizable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.metrics.PYMetricRegistry.PYMetricNameBuilder;
import py.monitor.customizable.repository.AttributeMetadata;
import py.monitor.jmx.repoter.notification.AlarmNotification;
import py.monitor.jmx.repoter.notification.PerformanceNotification;
import py.monitor.jmx.server.ResourceRelatedAttribute;
import py.monitor.pojo.management.helper.NotificableDynamicMBean;

/**
 * {@code CDNMBean} will be used as a template to create a dynamic MBean by a lots of MBean attributes that had been
 * restored in the {@code AttributeStore}.
 * 
 * After the derived class has been created, we can place it into {@code py.monitor.jmx.repoter.Reporter} so that the
 * {@code Reporter} can support 2 kinds of MBean: 1. Annotated POJO MBeans that are defined by java code.(POJO is just a
 * definition of MBean in JMX side) 2. Customisable MBeans that are defined by a lots of attributes.(we can customize
 * all performance task by this way!!)
 * 
 * 
 * <pre class="code">
 * </pre>
 * 
 * @author sxl
 * 
 * @remark 1. POJO means "Plain Ordinary Java Object". And it always be used to create a bean object in many
 *         architecture, such as JMX,Spring and so on
 * 
 *         2.CDNMBean means "Customizable Notificable Dynamic MBean"
 */

public class CustomizableMBean extends NotificableDynamicMBean {
    private static final Logger logger = LoggerFactory.getLogger(CustomizableMBean.class);
    private final MBeanInfo mbeanInfo;
    private final MBeanAttributeInfo[] attributes;
    private final MBeanOperationInfo[] operations;
    private final MBeanNotificationInfo[] notifications;
    private MBeanServer mbeanServer = null;

    /**
     * constructor of CNDMBean
     * 
     * @param expression
     *            JEXL expression string.
     */
    public CustomizableMBean(EndPoint endpoint, MBeanServer mbeanServer, long parentTaskId,
            List<ResourceRelatedAttribute> resourceRelatedAttributes) throws IntrospectionException, Exception {
        logger.debug("resourceRelatedAttributes are : {}", resourceRelatedAttributes);

        this.mbeanServer = mbeanServer;
        this.resourceRelatedAttributes = resourceRelatedAttributes;
        this.endpoint = endpoint;
        this.parentTaskId = parentTaskId;

        /*
         * attributes
         */
        if (resourceRelatedAttributes != null) {
            attributes = new MBeanAttributeInfo[resourceRelatedAttributes.size()];
            Iterator<ResourceRelatedAttribute> itResourceRelatedAttribut = resourceRelatedAttributes.iterator();
            for (int i = 0; i < attributes.length; i++) {
                ResourceRelatedAttribute resourceRelatedAttribute = itResourceRelatedAttribut.next();
                AttributeMetadata attributeMetadata = resourceRelatedAttribute.getAttributeMetadata();

                String resourceId = null;
                switch (attributeMetadata.getResourceType()) {
                case MACHINE:
                    resourceId = this.endpoint.getHostName();
                    break;
                case JVM:
                    resourceId = String.format("%s[%d]", endpoint.getHostName(), endpoint.getPort());
                    break;
                default:
                    resourceId = String.valueOf(resourceRelatedAttribute.getResourceId());
                    break;
                }
                String resourceRelatedName = new PYMetricNameBuilder(attributeMetadata.getMbeanObjectName())
                        .type(attributeMetadata.getResourceType()).id(resourceId).build() + "."
                        + attributeMetadata.getAttributeName();

                attributes[i] = new MBeanAttributeInfo(resourceRelatedName, // property name
                        attributeMetadata.getAttributeType(), // property type
                        attributeMetadata.getDescription(), // property description
                        true, // isReadable
                        true, // isWritable
                        false); // isIs
            }
        } else {
            attributes = null;
        }

        /*
         * operations
         */
        /**
         * need no operations <br>
         * <code>
         * 
         * int i = 0; operations = new MBeanOperationInfo[attributeMetadatas.size() * 2]; for (MBeanAttributeInfo
         * attribute : attributes) { // getter String getterName = getterPrifix + attribute.getName(); operations[i++] =
         * new MBeanOperationInfo(getterName, "Reload properties from class", null, // no parameters
         * attribute.getType(), MBeanOperationInfo.INFO);
         * 
         * // setter String setterName = setterPrifix + attribute.getName(); operations[i++] = new
         * MBeanOperationInfo(setterName, "Set properties to class", null, // no parameters attribute.getType(),
         * MBeanOperationInfo.INFO); }
         */
        operations = null;

        /*
         * 
         */
        notifications = new MBeanNotificationInfo[2];
        notifications[0] = new MBeanNotificationInfo(
                new String[] { PerformanceNotification.LIST_ATTRIBUTE_FOR_PERFORMANCE },
                PerformanceNotification.class.getName(),
                "This notification is emitted when the reset() method is called.");
        notifications[1] = new MBeanNotificationInfo(new String[] { AlarmNotification.LIST_ATTRIBUTE_FOR_ALARM },
                AlarmNotification.class.getName(), "This notification is emitted when the reset() method is called.");

        /*
         * MBeanInfo
         */
        mbeanInfo = new MBeanInfo(this.getClass().getName(), "Property Manager MBean", attributes, null, // constructors
                operations, notifications); // notifications
    }

    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        logger.debug("getAttribute {}", attribute);

        // find the data provider from MBeanServer. and invoke it's getter
        boolean attributeFound = false;
        for (ResourceRelatedAttribute resourceRelatedAttribute : resourceRelatedAttributes) {
            logger.debug("Current resource related attribute : {}", resourceRelatedAttribute);

            AttributeMetadata attributeMetadata = resourceRelatedAttribute.getAttributeMetadata();
            // if (attributeMetadata.getId() == Long.parseLong(attribute)) {

            String fullAttributeName = null;
            try {
                fullAttributeName = new PYMetricNameBuilder(attributeMetadata.getMbeanObjectName())
                        .type(attributeMetadata.getResourceType())
                        .id(String.valueOf(resourceRelatedAttribute.getResourceId())).build() + "."
                        + attributeMetadata.getAttributeName();
            } catch (Exception e) {
                logger.error("Caught an exception", e);
                continue;
            }

            if (fullAttributeName.equals(attribute)) {
                attributeFound = true;
                ObjectName mbeanObjectName = null;
                try {
                    String resourceId = null;
                    switch (attributeMetadata.getResourceType()) {
                    case MACHINE:
                        resourceId = this.endpoint.getHostName();
                        break;
                    case JVM:
                        resourceId = String.format("%s[%d]", endpoint.getHostName(), endpoint.getPort());
                        break;
                    default:
                        resourceId = String.valueOf(resourceRelatedAttribute.getResourceId());
                        break;
                    }

                    String resourceRelatedName = new PYMetricNameBuilder(attributeMetadata.getMbeanObjectName())
                            .type(attributeMetadata.getResourceType()).id(resourceId).build();
                    logger.debug("{}", resourceId);
                    logger.debug("mbeanObjectName: {}", resourceRelatedName);

                    mbeanObjectName = new ObjectName(resourceRelatedName);

                    String mbeanAttributeName = attributeMetadata.getAttributeName();
                    Object object = this.mbeanServer.getAttribute(mbeanObjectName, mbeanAttributeName);
                    logger.debug("getAttribute {} will return value {}", attribute, object);
                    return object;
                } catch (MalformedObjectNameException e) {
                    logger.error("There is no data provider defined in the attribute repository", e);
                    throw new MBeanException(null);
                } catch (InstanceNotFoundException e) {
                    logger.error("There is no mbean named {} in the mbean server", mbeanObjectName, e);
                    throw new MBeanException(null);
                } catch (Exception e) {
                    logger.error("There is no mbean named {} in the mbean server", mbeanObjectName, e);
                    throw new MBeanException(null);
                }
            }
        }

        if (!attributeFound) {
            throw new AttributeNotFoundException();
        }
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        logger.debug("setAttribute {}", attribute);

        // find the data provider from MBeanServer. and invoke it's setter
        boolean attributeFound = false;
        for (ResourceRelatedAttribute resourceRelatedAttribute : resourceRelatedAttributes) {
            logger.debug("Current attribute : {}", resourceRelatedAttribute);

            AttributeMetadata attributeMetadata = resourceRelatedAttribute.getAttributeMetadata();
            if (attributeMetadata.getAttributeName().equals(attribute.getName())) {
                attributeFound = true;
                ObjectName mbeanObjectName = null;
                try {

                    String resourceRelatedName = new PYMetricNameBuilder(attributeMetadata.getMbeanObjectName())
                            .type(attributeMetadata.getResourceType())
                            .id(String.valueOf(resourceRelatedAttribute.getResourceId())).build();

                    mbeanObjectName = new ObjectName(attributeMetadata.getMbeanObjectName());
                    logger.debug("Current object-name: {}, attribute: ", mbeanObjectName, attribute);
                    this.mbeanServer.setAttribute(mbeanObjectName, attribute);
                    break;
                } catch (MalformedObjectNameException e) {
                    logger.error("There is no data provider defined in the attribute repository");
                    throw new MBeanException(null);
                } catch (InstanceNotFoundException e) {
                    logger.error("There is no mbean named {} in the mbean server", mbeanObjectName);
                    throw new MBeanException(null);
                } catch (Exception e) {
                    logger.error("There is no mbean named {} in the mbean server", mbeanObjectName, e);
                    // throw new MBeanException(null);
                }
            }
        }

        if (!attributeFound) {
            logger.error("The attribute {} is not existed", attribute);
            throw new AttributeNotFoundException();
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        if (attributes == null)
            return new AttributeList();
        final List<Attribute> result = new ArrayList<Attribute>(attributes.length);
        for (String attr : attributes) {
            try {
                result.add(new Attribute(attr, this.getAttribute(attr)));
            } catch (Exception x) {
                logger.error("Caught an exception when get attribute {}", attr);
                continue;
            }
        }
        return new AttributeList(result);
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        if (attributes == null)
            return new AttributeList();
        final List<Attribute> result = new ArrayList<Attribute>(attributes.size());
        for (Object item : attributes) {
            Attribute attr = (Attribute) item;
            try {
                this.setAttribute(attr);
                result.add(new Attribute(attr.getName(), attr.getValue()));
            } catch (Exception x) {
                logger.error("Caught an exception when set attribute {}", attr);
                continue;
            }
        }
        return new AttributeList(result);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        logger.debug("invoke {}", actionName);
        return null;

        /**
         * <code> Method toInvoke = null; if (params == null) params = new Object[0]; if (signature == null) signature =
         * new String[0]; for (Method m : operations) { if (!m.getName().equals(actionName)) continue; final Class[] sig
         * = m.getParameterTypes(); if (sig.length == params.length) { if (sig.length == 0) toInvoke = m; else if
         * (signature.length == sig.length) { toInvoke = m; for (int i = 0; i < sig.length; i++) { if
         * (!sig[i].getName().equals(signature[i])) { toInvoke = null; break; } } } } if (toInvoke != null) break; } if
         * (toInvoke == null) throw new ReflectionException(new NoSuchMethodException(actionName)); try { return
         * toInvoke.invoke(this, params); } catch (IllegalArgumentException ex) { throw new ReflectionException(ex); }
         * catch (InvocationTargetException ex) { final Throwable cause = ex.getCause(); if (cause instanceof Exception)
         * throw new MBeanException((Exception) cause); throw new RuntimeErrorException((Error) cause); } catch
         * (IllegalAccessException ex) { throw new ReflectionException(ex); }
         */
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        return null;
    }

    @Override
    public void postRegister(Boolean registrationDone) {

    }

    @Override
    public void preDeregister() throws Exception {

    }

    @Override
    public void postDeregister() {

    }

    private MBeanAttributeInfo getAttributeInfo(String attributeName) throws AttributeNotFoundException {
        int i = 0;
        MBeanAttributeInfo attributeInfo = null;
        for (; i < attributes.length; ++i) {
            if (attributes[i].getName().equals(attributeName)) {
                attributeInfo = attributes[i];
                break;
            }
        }
        if (i == attributes.length) {
            logger.warn("Can't find the attribute in attribute set");
            throw new AttributeNotFoundException(attributeName);
        }

        return attributeInfo;
    }
}
