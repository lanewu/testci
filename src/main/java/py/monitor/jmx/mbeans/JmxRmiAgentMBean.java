package py.monitor.jmx.mbeans;

import java.util.List;
import java.util.Set;

import py.common.struct.EndPoint;
import py.monitor.customizable.repository.AttributeMetadata;
import py.monitor.jmx.server.ResourceRelatedAttribute;
import py.monitor.task.TimeSpan;

/**
 * We use this MBean as an remote proxy so that we can do operation (such as 'createSubTask') from client
 * 
 * @author sxl
 * 
 */
public interface JmxRmiAgentMBean {
    public static final String performanceDomain = "PerformanceReporter";
    public static final String ReporterCreaterName = "real-mbean:name=MBeanCreator";

    /**
     * Create sub-task in remote {@code MBeanServer}, and start it
     * 
     * @param parentTaskId
     *            id of parent-task,all sub-task are delivered from the parent-task.
     * @param period
     * @param runTime
     * @param attributes
     * @return
     * @throws Exception
     */
    public String createSubTask(long parentTaskId, long period, List<TimeSpan> runTime,
            List<ResourceRelatedAttribute> resourceRelatedAttribute) throws Exception;

    /**
     * unregist sub-task from remote {@code MBeanServer}
     * 
     * @param objectName
     *            object name of the sub-task in the {@code MBeanServer}
     * @throws Exception
     */
    public void destroySubTask(String objectName) throws Exception;

    /**
     * Get all MBean information from remote {@code MBeanServer}
     * 
     * @param domains
     *            the domains of MBeans.
     * @return MBeans information in the remote {@code MBeanServer}
     * @throws Exception
     */
    public Set<AttributeMetadata> getMbeans(Set<String> domains) throws Exception;

    /**
     * Set the host name to the remote object. <br>
     * Although we can use "InetAddress.getLocalHost().getHostAddress()" to get the address of local host, but it might
     * be the common address : 127.0.0.1. So, we need this method to set the real address to the remote object.
     * 
     * @param self
     *            it's IP-address:port here,such as 10.0.1.20:1234
     */
    // public void selfIs(EndPoint self);
}
