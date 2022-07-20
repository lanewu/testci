package py.monitor.pojo.management;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.pojo.management.helper.NotificableDynamicMBean;

/**
 * 
 * @author sxl
 * 
 * @history 1.sxl add the notification mechanism. After the old set attribute method has been invoked, we'll send the
 *          data to client immediately
 */

public abstract class PeriodicPojo extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(PeriodicPojo.class);
    private NotificableDynamicMBean realBean;

    /**
     * reporter switch
     * 
     * WARNING: DO NOT open this switch when use the POJO class as a data collector
     */
    private boolean autoReport = false;

    public NotificableDynamicMBean getRealBean() {
        return realBean;
    }

    public void setRealBean(NotificableDynamicMBean realBean) {
        this.realBean = realBean;
    }

    public boolean isAutoReport() {
        return autoReport;
    }

    public void setAutoReport(boolean needReportOut) {
        this.autoReport = needReportOut;
    }

    @Override
    public void run() {
        try {
            modifyData();

            logger.debug("Reporter switch : {}", autoReport);
            if (autoReport == true) {
                realBean.sendPerformanceNotify();
            }
        } catch (Exception e) {
            logger.error("Caught an exception", e);
        }
    }

    public void stop() {
        this.cancel();
    }

    protected abstract void modifyData() throws Exception;

}
