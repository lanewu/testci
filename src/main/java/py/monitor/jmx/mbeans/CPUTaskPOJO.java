package py.monitor.jmx.mbeans;

import static py.monitor.pojo.management.annotation.MBean.AutomaticType.ATTRIBUTE;
import static py.monitor.pojo.management.annotation.MBean.AutomaticType.OPERATION;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import javax.management.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.jmx.server.ResourceType;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.annotation.Description;
import py.monitor.pojo.management.annotation.MBean;
import py.monitor.pojo.management.annotation.ManagedAttribute;

/**
 * This class is used to get the information of CPU usage of the operating system and JVM
 * 
 * DO NOT use the the {@code java.lang.management.*}, because the data collected by this library is not exactly
 * 
 * @author sxl
 * 
 */
@MBean(objectName = "pojo-agent-JVM:name=CPUTask", resourceType = ResourceType.JVM, automatic = { ATTRIBUTE,
        OPERATION })
@Description("CPU MBean timer task")
public class CPUTaskPOJO extends PeriodicPojo {
    private static final Logger logger = LoggerFactory.getLogger(CPUTaskPOJO.class);

    private double cpuUsage;

    @Description(value = "Cpu usage")
    @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    public double getCpuUsage() {
        return cpuUsage;
    }

    @ManagedAttribute
    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    @Override
    protected void modifyData() throws Exception {
        OperatingSystemMXBean operatingSystemInfo = ManagementFactory.getOperatingSystemMXBean();
        this.getRealBean().setAttribute(new Attribute("cpuUsage", operatingSystemInfo.getSystemLoadAverage()));
    }
}
