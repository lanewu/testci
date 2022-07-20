package py.monitor.jmx.server;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.monitor.customizable.CustomizableMBean;
import py.monitor.pojo.management.exception.ManagementException;
import py.monitor.pojo.management.helper.MBeanRegistration;
import py.monitor.pojo.management.helper.NotificableDynamicMBean;

/**
 * The {@code PeriodicReporter} must use the same {@code MBeanServer} as {@code Collector} if you want to report some data by
 * JMX.
 * 
 * @author sxl
 * 
 */
public class PeriodicReporter extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(PeriodicReporter.class);
    private MBeanRegistration registration = null;
    private Timer timer;
    private long delay;
    private long period;
    private NotificableDynamicMBean mbean;
    private Switcher alarmSwitcher;
    private Switcher performanceSwitcher;
    private EndPoint endpoint;

    public static enum Switcher {
        ON, OFF;
    }

    /**
     * 
     * @param <E>
     * @param dataProvider
     *            the POJO class will be used to create a MBean class, and registed to the {@code MBeanServer}.
     * @return
     * @throws Exception
     */
    /**
     * <pre class="code">
     * public static Builder createBy(List<String> attributeNames) throws Exception { return new
     * Builder(attributeNames); }
     * 
     * public static Builder createBy(String expressionString) throws Exception { return new Builder(expressionString);
     * }
     */

    public static Builder createBy(long parentTaskId, List<ResourceRelatedAttribute> attributeMetadatas) {
        return new Builder(parentTaskId, attributeMetadatas);
    }

    /**
     * Reporter object builder, to make the reporter object creating much more easier.
     * 
     * @author sxl
     * 
     */
    public static class Builder {
        private MBeanRegistration registration;
        private MBeanServer mBeanServer;
        private ObjectName mbeanObjectName;
        private Timer timer;
        private long delay = 0;
        private long period = 1000;
        private List<ResourceRelatedAttribute> attributes;
        private final long parentTaskId;
        private EndPoint endpoint;
        // private AttributeStore attributeRepository;
        private CustomizableMBean dataProvider;
        private Switcher alarmSwitcher = Switcher.ON; // whether the notification of alarm should be sent.
        private Switcher performanceSwitcher = Switcher.ON; // whether the notification of performance should be sent.

        /**
         * <code>
         * 
         * private Builder(List<String> attributeNames) throws Exception { this.attributeMetadatas =
         * getAttributeMetadatas(attributeNames); }
         * 
         * private Builder(String expressionString) throws Exception { this.attributeMetadatas =
         * this.getAttributeMetadatas(expressionString); }
         */

        private Builder(long parentTaskId, List<ResourceRelatedAttribute> attributes) {
            this.attributes = attributes;
            this.parentTaskId = parentTaskId;
        }

        public Builder forRegistration(MBeanRegistration registration) {
            this.registration = registration;
            return this;
        }

        public Builder bindMBeanServer(MBeanServer mBeanServer) {
            this.mBeanServer = mBeanServer;
            return this;
        }

        public Builder objectName(ObjectName mbeanObjectName) {
            this.mbeanObjectName = mbeanObjectName;
            return this;
        }

        public Builder turn(Switcher performanceSwitcher, Switcher alarmSwitcher) {
            this.alarmSwitcher = alarmSwitcher;
            this.performanceSwitcher = performanceSwitcher;
            return this;
        }

        public Builder objectName(String mbeanObjectName) throws MalformedObjectNameException {
            this.mbeanObjectName = new ObjectName(mbeanObjectName);
            return this;
        }

        public Builder loadDataProvider(CustomizableMBean dataProvider) {
            this.dataProvider = dataProvider;
            return this;
        }

        public Builder bindTimer(Timer timer) {
            this.timer = timer;
            return this;
        }

        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        public Builder period(long period) {
            this.period = period;
            return this;
        }

        public Builder endpoint(EndPoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public PeriodicReporter build() throws Exception {
            checkObjectName();
            checkDataProvider();
            checkRegistration();
            checkTimer();
            checkEndpoint();

            return new PeriodicReporter(endpoint, registration, timer, dataProvider, delay, period, alarmSwitcher,
                    performanceSwitcher);
        }

        private void checkObjectName() throws Exception {
            if (mbeanObjectName == null) {
                this.mbeanObjectName = new ObjectName("pojo-agent-JVM:name=CustomizableMBean");
            } else {
                // do nothing
            }
        }

        private void checkDataProvider() throws Exception {
            if (dataProvider == null) {
                logger.warn("No data provider has been selected in, so we're going to create a new one");
                dataProvider = new CustomizableMBean(this.endpoint, this.mBeanServer, this.parentTaskId,
                        this.attributes);
            } else {
                // do nothing
            }
        }

        private void checkRegistration() throws Exception {
            if (registration == null) {
                if (mBeanServer == null) {
                    registration = new MBeanRegistration(endpoint, dataProvider, mbeanObjectName);
                } else {
                    registration = new MBeanRegistration(endpoint, dataProvider, mbeanObjectName, mBeanServer);
                }
            } else {
                // do nothing. use the "registration" gethered by builder.
            }
        }

        private void checkTimer() throws Exception {
            if (timer == null) {
                logger.warn("It is NOT good enough to create a timer just used by one single MBean");
                timer = new Timer();
            } else {
                // do nothing. use the "timer" gethered by builder.
            }
        }

        private void checkEndpoint() throws Exception {
            if (this.endpoint == null) {
                for (ResourceRelatedAttribute attribute : attributes) {
                    if (attribute.getAttributeMetadata().getResourceType() == ResourceType.MACHINE) {
                        logger.warn("There is at least one performance attribute's resource type is "
                                + "MACHINE,but we the resource id can't be specified");
                        throw new Exception();
                    }
                }
            }
        }
    }

    /**
     * Reporter constructor
     * 
     * @param registration
     * @param timer
     * @param pojo
     * @param filter
     * @param delay
     * @param period
     */
    private PeriodicReporter(EndPoint endpoint, MBeanRegistration registration, Timer timer, CustomizableMBean dataProvider,
            long delay, long period, Switcher alarmSwitcher, Switcher performanceSwitcher) {
        this.endpoint = endpoint;
        this.delay = delay;
        this.period = period;
        this.timer = timer;
        this.registration = registration;
        this.alarmSwitcher = alarmSwitcher;
        this.performanceSwitcher = performanceSwitcher;
    }

    public void start() throws Exception {
        mbean = registration.register();
        timer.schedule(this, delay, period);
    }

    public void stop() throws Exception {
        if (this.registration == null) {
            logger.warn("Timer task has not been started, no need to do 'stop' operation");
            return;
        }

        if (this.cancel() == false) {
            throw new Exception("Failed to stop the task as a timer task");
        }

        try {
            registration.unregister();
        } catch (ManagementException e) {
            logger.error("Faild to stop the task as a mbean task", e);
            throw e;
        }
    }

    @Override
    public void run() {
        try {
            if (performanceSwitcher == Switcher.ON) {
                this.mbean.sendPerformanceNotify();
            }

            // if (performanceSwitcher == Switcher.ON) {
            // this.mbean.sendAlarmNotify();
            // }
        } catch (Exception e) {
            logger.error("Caught an exception when send notification to JMX client");
        }
    }

    public MBeanRegistration getRegistration() {
        return registration;
    }

    public void setRegistration(MBeanRegistration registration) {
        this.registration = registration;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public NotificableDynamicMBean getMbean() {
        return mbean;
    }

    public void setMbean(NotificableDynamicMBean mbean) {
        this.mbean = mbean;
    }

    public Switcher getAlarmSwitcher() {
        return alarmSwitcher;
    }

    public void setAlarmSwitcher(Switcher alarmSwitcher) {
        this.alarmSwitcher = alarmSwitcher;
    }

    public Switcher getPerformanceSwitcher() {
        return performanceSwitcher;
    }

    public void setPerformanceSwitcher(Switcher performanceSwitcher) {
        this.performanceSwitcher = performanceSwitcher;
    }

}
