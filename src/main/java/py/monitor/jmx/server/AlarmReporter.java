package py.monitor.jmx.server;

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.monitor.alarmbak.AlarmMessageData;
import py.monitor.customizable.CustomizableMBean;
import py.monitor.pojo.management.exception.ManagementException;
import py.monitor.pojo.management.helper.MBeanRegistration;
import py.monitor.pojo.management.helper.NotificableDynamicMBean;
import py.monitor.utils.Service;

public class AlarmReporter extends Service {
    private static final Logger logger = LoggerFactory.getLogger(AlarmReporter.class);
    public static final String AlarmReporterObjectName = "pojo-agent-alarm:name=AlarmReporter";
    private MBeanRegistration registration = null;
    private NotificableDynamicMBean mbean;

    public static Builder create() {
        return new Builder();
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
        private CustomizableMBean dataProvider;
        private EndPoint endpoint;

        private Builder() {
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

        public Builder objectName(String mbeanObjectName) throws MalformedObjectNameException {
            this.mbeanObjectName = new ObjectName(mbeanObjectName);
            return this;
        }

        public Builder endpoint(EndPoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public AlarmReporter build() throws Exception {
            checkObjectName();
            checkDataProvider();
            checkRegistration();
            checkEndpoint();

            return new AlarmReporter(registration);
        }

        private void checkObjectName() throws Exception {
            if (mbeanObjectName == null) {
                this.mbeanObjectName = new ObjectName(AlarmReporterObjectName);
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

        private void checkDataProvider() throws Exception {
            if (dataProvider == null) {
                logger.warn("No data provider has been selected in, so we're going to create a new one");
                dataProvider = new CustomizableMBean(this.endpoint, this.mBeanServer, 0,
                        new ArrayList<ResourceRelatedAttribute>());
            } else {
                // do nothing
            }
        }

        private void checkEndpoint() throws Exception {
            if (this.endpoint == null) {
                throw new Exception();
            }
        }
    }

    /**
     * 
     * @param endpoint
     * @param registration
     */
    private AlarmReporter(MBeanRegistration registration) {
        this.registration = registration;
    }

    public MBeanRegistration getRegistration() {
        return registration;
    }

    public void setRegistration(MBeanRegistration registration) {
        this.registration = registration;
    }

    public NotificableDynamicMBean getMbean() {
        return mbean;
    }

    public void setMbean(NotificableDynamicMBean mbean) {
        this.mbean = mbean;
    }

    @Override
    protected void onStart() throws Exception {
        mbean = registration.register();
    }

    @Override
    protected void onStop() throws Exception {
        if (this.registration == null) {
            logger.warn("Timer task has not been started, no need to do 'stop' operation");
            return;
        }

        try {
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

    public void report(AlarmMessageData data) throws Exception {
        try {
            this.mbean.sendAlarmNotify(data);
        } catch (Exception e) {
            logger.error("Caught an exception when sending alarm data to monitorcenter");
            throw e;
        }
    }
}
