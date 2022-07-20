package py.monitor.pojo.management.helper;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.monitor.customizable.CustomizableMBean;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.exception.ManagementException;

/**
 * 
 * This class assists in registering MBeans with an MBeanServer.
 * <p>
 * 
 * This class, unfortunately, has a name that may cause confusion, since it doesn't implement the
 * {@link javax.management.MBeanRegistration} interface.
 * 
 * @author sxl
 * 
 */
public class MBeanRegistration {
    private static final Logger logger = LoggerFactory.getLogger(MBeanRegistration.class);
    private final Object object;
    private final ObjectName mBeanObjectName;
    private final MBeanServer mBeanServer;
    private final EndPoint endpoint;

    /**
     * @param mBean
     *            an MBean instance annotated with {@link @MBean} containing an objectName attribute
     * @throws MalformedObjectNameException
     */
    public MBeanRegistration(EndPoint endpoint, Object mBean) throws MalformedObjectNameException {
        this(endpoint, mBean, new ObjectNameBuilder().withEndpoint(endpoint).withObjectName(mBean.getClass()).build());
    }

    /**
     * @param mBean
     *            an MBean instance in the form of a traditional MBean (implementing a sibling *MBean interface) or an
     *            MXBean (implementing an interface annotated with {@code @MXBean}), or an instance implementing the
     *            DynamicMBean interface.
     * @param mBeanObjectName
     *            the object name with which {@code mBean} will be registered
     */
    public MBeanRegistration(EndPoint endpoint, Object mBean, ObjectName mBeanObjectName) {
        this.endpoint = endpoint;
        this.object = mBean;
        this.mBeanObjectName = mBeanObjectName;
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public MBeanRegistration(EndPoint endpoint, Object mBean, MBeanServer mBeanServer)
            throws MalformedObjectNameException {
        this.endpoint = endpoint;
        this.object = mBean;
        this.mBeanObjectName = new ObjectNameBuilder().withEndpoint(endpoint).withObjectName(mBean.getClass()).build();
        this.mBeanServer = mBeanServer;
    }

    /**
     * @param mBean
     *            an MBean instance in the form of a traditional MBean (implementing a sibling *MBean interface) or an
     *            MXBean (implementing an interface annotated with {@code @MXBean}), or an instance implementing the
     *            DynamicMBean interface.
     * @param mBeanObjectName
     *            the object name with which {@code mBean} will be registered
     * @param mBeanServer
     *            the MBeanServer to register the mBean in
     */
    public MBeanRegistration(EndPoint endpoint, Object mBean, ObjectName mBeanObjectName, MBeanServer mBeanServer) {
        this.endpoint = endpoint;
        this.object = mBean;
        this.mBeanObjectName = mBeanObjectName;
        this.mBeanServer = mBeanServer;
    }

    /**
     * Register the MXBean. If the registration fails, a WARN message is logged
     * 
     * @throws java.beans.IntrospectionException
     * @throws IntrospectionException
     * @throws NotCompliantMBeanException
     * @throws MBeanRegistrationException
     * @throws InstanceAlreadyExistsException
     */
    public NotificableDynamicMBean register() throws ManagementException {
        try {
            NotificableDynamicMBean dynamicMBean = null;
            if (object instanceof PeriodicPojo) {
                dynamicMBean = new IntrospectedDynamicMBean((PeriodicPojo) object);
            } else if (object instanceof CustomizableMBean) {
                dynamicMBean = (NotificableDynamicMBean) object;
            } else {
                throw new ManagementException();
            }
            dynamicMBean.setEndpoint(endpoint);
            logger.debug("Going to regist new MBean to MBeanServer. MBean name: {}", mBeanObjectName);
            mBeanServer.registerMBean(dynamicMBean, mBeanObjectName);
            return dynamicMBean;
        } catch (Exception e) {
            throw new ManagementException(e);
        }
    }

    /**
     * Unregister the MXBean. If the unregistration fails, a WARN message is logged
     * 
     * @throws InstanceNotFoundException
     * @throws MBeanRegistrationException
     */
    public void unregister() throws ManagementException {
        try {
            logger.debug("Going to unregitster mbean: {}", mBeanObjectName);
            mBeanServer.unregisterMBean(mBeanObjectName);
        } catch (Exception e) {
            throw new ManagementException(e);
        }
    }

    public Object getPojo() {
        return object;
    }

    public ObjectName getmBeanObjectName() {
        return mBeanObjectName;
    }

    public MBeanServer getmBeanServer() {
        return mBeanServer;
    }

    public EndPoint getEndpoint() {
        return endpoint;
    }

}
