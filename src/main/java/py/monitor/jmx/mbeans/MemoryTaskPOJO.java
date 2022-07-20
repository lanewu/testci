package py.monitor.jmx.mbeans;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import javax.management.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.jmx.server.ResourceType;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.annotation.Description;
import py.monitor.pojo.management.annotation.MBean;
import py.monitor.pojo.management.annotation.ManagedAttribute;

/**
 * this class will be used to collect the performance&alert information, and send it to the JMX client by notification.
 * 
 * @author sxl
 * 
 */

@MBean(objectName = "pojo-agent-JVM:name=MemoryTask", resourceType = ResourceType.JVM)
@Description("Memory MBean timer task")
public class MemoryTaskPOJO extends PeriodicPojo {
    private static final Logger logger = LoggerFactory.getLogger(MemoryTaskPOJO.class);

    private long init;
    private long used;
    private long committed;
    private long max;

    @Description(value = "Initial memory size")
    @ManagedAttribute(unitOfMeasurement = "Byte")
    public long getInit() {
        return init;
    }

    @ManagedAttribute
    public void setInit(long init) {
        this.init = init;
    }

    @Description(value = "Been used JVM memory")
    @ManagedAttribute(unitOfMeasurement = "Byte")
    public long getUsed() {
        return used;
    }

    @ManagedAttribute
    public void setUsed(long used) {
        this.used = used;
    }

    @Description(value = "Committed JVM memory")
    @ManagedAttribute(unitOfMeasurement = "Byte")
    public long getCommitted() {
        return committed;
    }

    @ManagedAttribute
    public void setCommitted(long committed) {
        this.committed = committed;
    }

    @Description(value = "Max JVM memory size")
    @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "(%)percentage")
    public long getMax() {
        return max;
    }

    @ManagedAttribute
    public void setMax(long max) {
        this.max = max;
    }

    @Override
    protected void modifyData() throws Exception {
        MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        this.getRealBean().setAttribute(new Attribute("init", memUse.getInit()));
        this.getRealBean().setAttribute(new Attribute("used", memUse.getUsed()));
        this.getRealBean().setAttribute(new Attribute("committed", memUse.getCommitted()));
        this.getRealBean().setAttribute(new Attribute("max", memUse.getMax()));
    }

    @Override
    public String toString() {
        return "MemoryTaskPOJO [init=" + init + ", used=" + used + ", committed=" + committed + ", max=" + max + "]";
    }

}
