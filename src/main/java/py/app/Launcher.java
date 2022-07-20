package py.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import py.metrics.PYMetricConfigruation;
import py.metrics.PYMetricRegistry;
import py.monitor.jmx.server.JmxAgent;
import py.processmanager.PMDB;
import py.processmanager.exception.PMDBPathNotExist;
import py.processmanager.utils.PMUtils;

/**
 * this class read spring configuration files and initialize beans in those files
 *
 * @author chenlia
 * @history 1.Start a {@code JmxAgent} for each service to get the performance data by sxl
 */
public abstract class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    /*
     * Holder of beans used in a service project.
     */
    protected String beansHolder;

    /*
     * lever for log,you can use this param to turn off the log
     */
    protected Level logLevel;

    /*
     * this switcher is used to enable the log or not true: open log function false: close log function
     */
    protected boolean logSwitcher = true;

    /*
     * Running path of a service which is usually parent folder of starting script locating folder "bin".
     */
    protected String serviceRunningPath;

    public Launcher(String beansHolder, String serviceRunningPath) {
        // set log from property file
        if (!logSwitcher) {
            // if the switcher is false,we shall turn the log function off
            logLevel = Level.WARN;
            LogManager.getRootLogger().setLevel(logLevel);
        }

        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        this.beansHolder = beansHolder;
        this.serviceRunningPath = serviceRunningPath;
    }

    /**
     * Generate an application context from specific beans holder whose type is a xml file or a class.
     *
     * @return
     * @throws Exception
     */
    protected ApplicationContext genAppContext() throws Exception {
        String metricEnable = System.getProperty("metric.enable");
        logger.warn("metricEnable: {}", metricEnable);

        // check if enable metric in system
        if (metricEnable != null && Boolean.parseBoolean(metricEnable)) {
            // init metric properties
            String metricEnableProfileProperty = System.getProperty("metric.enable.profiles");
            logger.warn("metric reporter type? {}", metricEnableProfileProperty);
            AnnotationConfigApplicationContext metricContext = new AnnotationConfigApplicationContext();
            if (metricEnableProfileProperty != null) {
                String[] metricEnableProfiles = metricEnableProfileProperty.split(",");
                logger.warn("metricEnableProfiles: {}", Arrays.asList(metricEnableProfiles));
                metricContext.getEnvironment().setActiveProfiles(metricEnableProfiles);
            }
            metricContext.register(PYMetricConfigruation.class);
            metricContext.refresh();
        }

        logger.warn("metric enable? {}", PYMetricRegistry.getMetricRegistry().isEnableMetric());

        if (beansHolder.contains(".xml")) {
            ApplicationContext context = new ClassPathXmlApplicationContext(beansHolder);
            return context;
        } else if (beansHolder.contains(".class")) {
            logger.debug("beanHolder : {}", beansHolder);
            int postfixPos = beansHolder.indexOf(".class");
            String className = beansHolder.substring(0, postfixPos);
            Class<?> contextClass = Class.forName(className);

            ApplicationContext context = new AnnotationConfigApplicationContext(contextClass);
            return context;
        }

        return null;
    }

    /**
     * Backup process id of the service which is launched at this time.
     *
     * @throws Exception
     */
    protected void backupServiceProcessId() {
        // backup service process pid
        final int currentProcessPid = PMUtils.getCurrentProcessPid();
        String pidFilePath = serviceRunningPath + File.separator + PMDB.SERVICE_PID_NAME;
        try {
            Files.deleteIfExists(Paths.get(pidFilePath));
        } catch (IOException e) {
            logger.debug("Failed to delete service status file when backup service status");
            System.exit(0);
        }
        boolean backupPid = false;
        PMDB pmdb;
        try {
            pmdb = PMDB.build(Paths.get(serviceRunningPath));
            backupPid = pmdb.save(PMDB.SERVICE_PID_NAME, String.valueOf(currentProcessPid));
            // set this log level to warn, because it's important to record a service start
            logger.warn("Correctly save process Id:{} to SPid file", currentProcessPid);
        } catch (PMDBPathNotExist e) {
            logger.error("fail to backup service process pid");
            try {
                Files.deleteIfExists(Paths.get(pidFilePath));
            } catch (IOException e1) {
                logger.debug("Failed to delete service status file when fail to start service");
            } finally {
                System.exit(0);
            }
        }
        if (!backupPid) {
            logger.warn("Failed to backup currentProcessPid to file ,exit directly");
            System.exit(0);
        }
    }

    // basic class method,do nothing
    protected abstract void startAppEngine(ApplicationContext appContext);

    // start monitor agent
    // protected abstract void startMonitorAgent(ApplicationContext appContext) throws Exception;
    protected void startMonitorAgent(ApplicationContext appContext) throws Exception {
        try {
            JmxAgent jmxAgent = appContext.getBean(JmxAgent.class);
            jmxAgent.start();
        } catch (Exception e) {
            logger.error("Caught an exception when start jmx-agent", e);
            System.exit(1);
        }
    }

    /**
     * An example launch method to launch service in pengyun system. This method is encouraged to be override in subclass
     * which extending this class.
     */
    public void launch() {
        try {
            // get app context,and start app engine
            ApplicationContext appContext = genAppContext();
            startAppEngine(appContext);

            /*
             * After inquiry from "information group", we know that functionality (report business relative metrics) of
             * JMX agent has been moved to system daemon. And JMX service is launched by configuring JVM option to
             * service launching script or "JavaProcessBuilder". The JMX option for JVM has prefix of
             * "com.sun.management.jmxremote".
             */
            // start monitor agent
            // startMonitorAgent(appContext);

            // backup service process id to file
            backupServiceProcessId();
        } catch (Exception e) {
            logger.error("Caught an exception when lauch service running in {}", serviceRunningPath, e);
            System.exit(1);
        }
    }
}
