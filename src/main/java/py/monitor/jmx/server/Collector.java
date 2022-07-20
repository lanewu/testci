package py.monitor.jmx.server;

import java.util.Timer;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.exception.ManagementException;
import py.monitor.pojo.management.helper.MBeanRegistration;
import py.monitor.pojo.management.helper.NotificableDynamicMBean;
import py.monitor.utils.Service;

/**
 * This class will be used to create a scheduler runnable MBean tasks. These tasks will collect the data which defines
 * in the POJO class and update it into the MBean class who is contained by the {@code MBeanServer}
 * 
 * I tried to create a common method{@code run} to set the mbean attributions, but it seems unable to set the different
 * data to the different attributions
 * 
 * I use "Builder pattern" to make the creating of task much more easier.
 * 
 * @problems: 1.timer must be stopped if the timer is created inside {@code Reporter}
 * 
 * @author sxl
 * 
 */
public class Collector extends Service {
    private static final Logger logger = LoggerFactory.getLogger(Collector.class);
    private MBeanRegistration registration = null;
    private Timer timer;
    private long delay;
    private long period;
    private String filter;
    private PeriodicPojo pojo;
    private NotificableDynamicMBean mbean;

    /**
     * 
     * @param pojo
     *            the POJO class will be used to create a MBean class, and registed to the {@code MBeanServer}.
     * @return
     */
    public static Builder createBy(PeriodicPojo pojo) {
        return new Builder(pojo);
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
        private final PeriodicPojo pojo;
        private ObjectName mBeanObjectName;
        private Timer timer;
        private String filter = new String("");
        private long delay = 0;
        private long period = 1000;
        private EndPoint endpoint;

        private Builder(PeriodicPojo pojo) {
            this.pojo = pojo;
        }

        public Builder forRegistration(MBeanRegistration registration) {
            this.registration = registration;
            return this;
        }

        public Builder bindMBeanServer(MBeanServer mBeanServer) {
            this.mBeanServer = mBeanServer;
            return this;
        }

        public Builder objectName(ObjectName mBeanObjectName) {
            this.mBeanObjectName = mBeanObjectName;
            return this;
        }

        public Builder bindTimer(Timer timer) {
            this.timer = timer;
            return this;
        }

        public Builder filter(String filter) {
            this.filter = filter;
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

        public Collector build() throws Exception {
            checkRegistration();
            checkTimer();

            return new Collector(endpoint, registration, timer, pojo, filter, delay, period);
        }

        private void checkRegistration() throws Exception {
            if (registration == null) {
                if (pojo == null) {
                    throw new Exception("puppetOfMBean can't be null");
                } else {
                    if (mBeanObjectName == null) {
                        if (mBeanServer != null) {
                            registration = new MBeanRegistration(endpoint, pojo, mBeanServer);
                        } else {
                            registration = new MBeanRegistration(endpoint, pojo);
                        }
                    } else {
                        if (mBeanServer == null) {
                            registration = new MBeanRegistration(endpoint, pojo, mBeanObjectName);
                        } else {
                            registration = new MBeanRegistration(endpoint, pojo, mBeanObjectName, mBeanServer);
                        }
                    }
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
    private Collector(EndPoint endpoint, MBeanRegistration registration, final Timer timer, PeriodicPojo pojo,
            String filter, long delay, long period) {
        this.delay = delay;
        this.period = period;
        this.filter = filter;
        this.timer = timer;
        this.registration = registration;
        this.pojo = pojo;
    }

    /**
     * 1.regist MBean to {@code MBeanServer} <br>
     * 2.Transition NEW -> STARTING
     * 
     * @throws Exception
     */
    protected void onStart() throws Exception {
        mbean = registration.register();
        pojo.setRealBean(mbean);

        if (timer != null) {
            timer.schedule(pojo, delay, period);
        }
    }

    /**
     * stop task,including: <br>
     * 1.stop from timer management <br>
     * 2.stop from MBean server management
     * 
     * @throws Exception
     */
    protected void onStop() throws Exception {
        logger.debug("Going to stop the collector");
        if (this.registration == null) {
            logger.warn("Timer task has not been started, no need to do 'stop' operation");
            return;
        }

        if (!pojo.cancel()) {
            throw new Exception("Failed to stop the task as a timer task");
        }

        if (timer != null) {
            timer.cancel();
        }

        try {
            logger.debug("Going to unregister the collector: {}", this.mbean);
            registration.unregister();
        } catch (ManagementException e) {
            logger.error("Faild to stop the task as a mbean task", e);
            throw e;
        }
    }

    @Override
    protected void onPause() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onResume() throws Exception {
        // TODO Auto-generated method stub

    }

}
