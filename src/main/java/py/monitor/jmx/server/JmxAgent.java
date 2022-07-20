package py.monitor.jmx.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;

import javax.management.BadAttributeValueExpException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadStringOperationException;
import javax.management.InvalidApplicationException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.app.context.AppContext;
import py.common.PyService;
import py.common.RequestIdBuilder;
import py.instance.PortType;
import py.metrics.PYMetricNameHelper;
import py.monitor.customizable.repository.AttributeMetadata;
import py.monitor.exception.NotExistedException;
import py.monitor.jmx.mbeans.CPUTaskPOJO;
import py.monitor.jmx.mbeans.JmxRmiAgent;
import py.monitor.jmx.mbeans.JmxRmiAgentMBean;
import py.monitor.jmx.mbeans.MemoryTaskPOJO;
import py.monitor.jmx.mbeans.RealMachineInfomation;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.helper.DescriptionEnDecoder;
import py.monitor.task.TimeSpan;
import py.monitor.utils.Service;

/**
 * This class is very important for the whole "py-monitor-system".
 * 
 * In order to create the sub-performance-task, there are 2 ways that we can take: <br>
 * 1. add thrift interface to all of the services.<br>
 * 2. create a MBean which can be use to create all of the others monitor MBeans.
 * 
 * obviously, it's much better to take the second way,so we create a MBean POJO just like {@code MbeanCreaterPOJO}.
 * 
 * All the functions of this class are as follows: <br>
 * 1. create a {@code MBeanServer}. <br>
 * 2. create a RMI connector,the RMI connector will be registed to the {@code MBeanServer}.
 * 
 * @author sxl
 * 
 * @remark 1.
 */
public class JmxAgent extends Service {
    private static final Logger logger = LoggerFactory.getLogger(JmxAgent.class);
    private static final String perfomanceReporterPrefix = "PerformanceReporter:name=";
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
    private AppContext appContext;

    // alert rules contains all of the alert threshold & duration
    private List<String> alertRules;

    // {@code performanceTaskContext} defines all the attributes which will be
    // monitored by the performance task.Each
    // of the element is a JMX attribute that should be reported.
    // List<performanceTaskAttribute> performanceTaskContext;
    private List<List<String>> performanceTaskContext;

    // timer for schedule task
    private Timer timer;

    // jmx connector
    private JMXConnectorServer jmxConnectorServer;

    // mbean creator. use this to create the performance task as MBeans
    private JmxRmiAgent mbeanCreator;

    // performance reporters
    private final List<PeriodicReporter> performanceReporters = new ArrayList<PeriodicReporter>();

    // alarm reporter
    private AlarmReporter alarmReporter;

    // collectors
    private final List<Collector> collectors = new ArrayList<Collector>();

    // MBeanServer
    private MBeanServer mbeanServer;

    private Registry registry;

    private static final String registryName = "jmxrmi";

    private boolean hasBeenRegisted = false;

    private boolean jmxSwitcher = true;

    private static class LazyHolder {
        private static final JmxAgent singletonInstance = new JmxAgent();
    }

    public static JmxAgent getInstance() {
        return LazyHolder.singletonInstance;
    }

    private JmxAgent() {
        super();
    }

    /**
     * use this method to add performance & alarm reporters dynamically
     * 
     * @step 1.get attribute names by attribute id, the names must be the MBean attribute name, not the alias for the
     *       reporter MBean attribute.<br>
     *       2.create
     * 
     * @param parentTaskId
     * @param period
     * @param runTime
     * @param attributeIds
     * @throws Exception
     */
    public String addPerformanceReporter(long parentTaskId, long period, List<TimeSpan> runTime,
            List<ResourceRelatedAttribute> attributes) throws Exception {
        logger.debug("period : {}, runTime : {}, attributes : {}", period, runTime, attributes);
        if (period < 1 || runTime.size() == 0 || attributes.size() == 0) {
            logger.error("Illegal parmeters. parameters are: {},{},{}", period, runTime, attributes);
            throw new Exception();
        }

        // create reporter
        logger.debug("AppContext: {}", appContext);
        String objectName = perfomanceReporterPrefix + UUID.randomUUID().toString();
        PeriodicReporter performanceReporter = PeriodicReporter.createBy(parentTaskId, attributes)
                .endpoint(appContext.getMainEndPoint()).objectName(objectName).bindMBeanServer(mbeanServer)
                .bindTimer(timer).period(period).build();
        performanceReporter.start();
        performanceReporters.add(performanceReporter);

        return objectName;
    }

    /**
     * remove the reporter & unregist the MBean from {@code MBeanServer}
     * 
     * @param objectName
     *            object name in {@code MBeanServer}
     * @throws NotExistedException
     *             The sub-task which will be removed is not existed
     * @throws Exception
     *             other errors
     */
    public void removePerformanceReporter(String objectName) throws NotExistedException, Exception {
        boolean found = false;
        for (PeriodicReporter reporter : performanceReporters) {

            String objectNameInMBeanServer = reporter.getRegistration().getmBeanObjectName().getCanonicalName();
            logger.debug("object name in mbean server is : {}", objectNameInMBeanServer);

            if (objectName.equals(objectNameInMBeanServer)) {
                reporter.stop();
                performanceReporters.remove(reporter);
                found = true;
                break;
            }
        }

        if (!found) {
            logger.warn("performance sub-task mbean not existed");
            throw new NotExistedException();
        }
    }

    /**
     * get
     * 
     * @param domains
     * @return
     * @throws Exception
     */
    public Set<AttributeMetadata> getMbeans(Set<String> domains) throws Exception {
        Set<AttributeMetadata> attributes = new HashSet<AttributeMetadata>();
        logger.debug("All observed domains are : {}. AppContext is {}", domains, appContext);
        for (String domain : domains) {
            logger.debug("Current domain is : {}", domain);
            Set<ObjectInstance> objectInstances = mbeanServer.queryMBeans(new ObjectName(domain + ":*"),
                    new QueryExp() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean apply(ObjectName name)
                                throws BadStringOperationException, BadBinaryOpValueExpException,
                                BadAttributeValueExpException, InvalidApplicationException {
                            return true;
                        }

                        @Override
                        public void setMBeanServer(MBeanServer s) {
                        }
                    });

            logger.debug("objectInstances size is {}", objectInstances.size());
            for (ObjectInstance objectInstance : objectInstances) {
                MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(objectInstance.getObjectName());
                MBeanAttributeInfo[] attributeInfo = mbeanInfo.getAttributes();
                for (int i = 0; i < attributeInfo.length; ++i) {
                    PYMetricNameHelper<String> resourceIdentify = new PYMetricNameHelper<String>(String.class,
                            objectInstance.getObjectName().toString());
                    if (resourceIdentify.getType() == null || resourceIdentify.getType().equals(ResourceType.NONE)) {
                        continue;
                    }

                    AttributeMetadata attribute = new AttributeMetadata();

                    attribute.setId(RequestIdBuilder.get());
                    attribute.setMbeanObjectName(objectInstance.getObjectName().toString());
                    attribute.setAttributeName(attributeInfo[i].getName());
                    attribute.setAttributeType(attributeInfo[i].getType());

                    DescriptionEnDecoder descriptionEnDecoder = new DescriptionEnDecoder();
                    descriptionEnDecoder.formatBy(attributeInfo[i].getDescription());
                    attribute.setRange(descriptionEnDecoder.getRange());
                    attribute.setUnitOfMeasurement(descriptionEnDecoder.getUnitOfMeasurement());
                    attribute.setDescription(descriptionEnDecoder.getDescription());

                    Set<String> services = new HashSet<String>();
                    services.add(appContext.getInstanceName());
                    attribute.setServices(services);

                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }

    /**
     * 1. create & register {@code MbeanCreaterPOJO} <br>
     * 2. create
     */
    protected void onStart() throws Exception {
        timer = new Timer();

        int port = appContext.getEndPointByServiceName(PortType.MONITOR).getPort();
        String hostName = appContext.getEndPointByServiceName(PortType.MONITOR).getHostName();

        // export ip for client to connect with
        System.setProperty("java.rmi.server.hostname", hostName);
        
        logger.debug("Has mbean-server been registed? : {}", hasBeenRegisted);
        // create a jmx mbean server here and place it into the scheduled task
        if (!hasBeenRegisted) {
            registry = LocateRegistry.createRegistry(port);
            hasBeenRegisted = true;
        }
        mbeanServer = ManagementFactory.getPlatformMBeanServer();

        // create mbean and register mbean
        String sUrl = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/%s", hostName, port, registryName);
        logger.debug("string: {}, hostName: {}, port: {}, registryName: {}", sUrl, hostName, port, registryName);
        JMXServiceURL url = new JMXServiceURL(sUrl);

        jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbeanServer);
        jmxConnectorServer.start();

        ObjectName name = new ObjectName(JmxRmiAgentMBean.ReporterCreaterName);
        mbeanCreator = new JmxRmiAgent();
        mbeanCreator.setAgent(this);
        mbeanServer.registerMBean(mbeanCreator, name);
        
        /*
         * self-defined JVM POJO collector
         */
        // Memory collector
        logger.debug("AppContext: {}, DIH service name: {}", appContext, PyService.DIH.name());
        PeriodicPojo memoryTaskPojo = new MemoryTaskPOJO();
        memoryTaskPojo.setAutoReport(false);
        Collector memoryCollector = Collector.createBy(memoryTaskPojo)
                .endpoint(appContext.getEndPointByServiceName(PortType.CONTROL)).bindMBeanServer(mbeanServer)
                .bindTimer(timer).period(3000).build();
        collectors.add(memoryCollector);
        memoryCollector.start();

        // CPU collector
        PeriodicPojo cpuTaskPojo = new CPUTaskPOJO();
        cpuTaskPojo.setAutoReport(false);
        Collector cpuCollector = Collector.createBy(cpuTaskPojo)
                .endpoint(appContext.getEndPointByServiceName(PortType.CONTROL)).bindMBeanServer(mbeanServer)
                .bindTimer(timer).period(3000).build();
        collectors.add(cpuCollector);
        cpuCollector.start();

        // real machine information collector, only for DIH
        if (appContext.getInstanceName().equals(PyService.DIH.name())) {
            RealMachineInfomation realMachinePojo = new RealMachineInfomation();
            cpuTaskPojo.setAutoReport(false);
            Collector realMachineInfoCollector = Collector.createBy(realMachinePojo)
                    .endpoint(appContext.getEndPointByServiceName(PortType.CONTROL)).bindMBeanServer(mbeanServer)
                    .bindTimer(timer).period(3000).build();
            collectors.add(realMachineInfoCollector);
            realMachineInfoCollector.start();
        }

        /*
         * metrics collector
         */
        // metrics collector is auto started by the metrics library which we've
        // used it to get our system metrics
        // information

        // alarm reporter
        alarmReporter = AlarmReporter.create().endpoint(appContext.getEndPointByServiceName(PortType.CONTROL))
                .bindMBeanServer(mbeanServer).build();
        alarmReporter.start();

    }

    @Override
    protected void onStop() throws Exception {
        for (PeriodicReporter performanceReporter : performanceReporters) {
            try {
                performanceReporter.stop();
            } catch (Exception e) {
                logger.error("Caught an exception", e);
                continue;
            }
        }

        for (Collector collector : collectors) {
            try {
                collector.stop();
            } catch (Exception e) {
                logger.error("Caught an exception", e);
            }
        }

        alarmReporter.stop();

        registry.unbind(registryName);
        timer.cancel();

        try {
            jmxConnectorServer.stop();
        } catch (IOException e) {
            logger.error("Caught an exception", e);
            throw e;
        }

        // unregister {@code MBeanCreator}
        ObjectName name = new ObjectName(JmxRmiAgentMBean.ReporterCreaterName);
        mbeanServer.unregisterMBean(name);

    }

    @Override
    protected boolean isJmxAgentSwitcherOn() throws Exception {
        return isJmxSwitcher();
    }

    @Override
    protected void onPause() throws Exception {

    }

    @Override
    protected void onResume() throws Exception {

    }

    public List<String> getAlertRules() {
        return alertRules;
    }

    public void setAlertRules(List<String> alertRules) {
        this.alertRules = alertRules;
    }

    public List<List<String>> getPerformanceTaskContext() {
        return performanceTaskContext;
    }

    public void setPerformanceTaskContext(List<List<String>> performanceTaskContext) {
        this.performanceTaskContext = performanceTaskContext;
    }

    public List<PeriodicReporter> getPerformanceReporters() {
        return performanceReporters;
    }

    public Timer getTimer() {
        return timer;
    }

    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public AlarmReporter getAlarmReporter() {
        return alarmReporter;
    }

    public void setAlarmReporter(AlarmReporter alarmReporter) {
        this.alarmReporter = alarmReporter;
    }

    public boolean isJmxSwitcher() {
        return jmxSwitcher;
    }

    public void setJmxSwitcher(boolean jmxSwitcher) {
        this.jmxSwitcher = jmxSwitcher;
    }
}
