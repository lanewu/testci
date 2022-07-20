package py.monitor.jmx.mbeans;

import static py.monitor.pojo.management.annotation.MBean.AutomaticType.ATTRIBUTE;
import static py.monitor.pojo.management.annotation.MBean.AutomaticType.OPERATION;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.alarmbak.AlarmLevel;
import py.monitor.alarmbak.AlarmMessageData;
import py.monitor.alarmbak.AlarmMessageData.AlarmOper;
import py.monitor.jmx.server.AlarmReporter;
import py.monitor.jmx.server.JmxAgent;
import py.monitor.jmx.server.ResourceType;
import py.monitor.pojo.management.PeriodicPojo;
import py.monitor.pojo.management.annotation.Description;
import py.monitor.pojo.management.annotation.MBean;
import py.monitor.pojo.management.annotation.ManagedAttribute;

/**
 * Use this class as a real machine information collector
 * 
 * @author carol
 *
 */

@MBean(objectName = "pojo-agent-MACHINE:name=RealMachineInfomation", resourceType = ResourceType.MACHINE, automatic = {
        ATTRIBUTE, OPERATION })
@Description("Memory MBean timer task")
public class RealMachineInfomation extends PeriodicPojo {
    private static final Logger logger = LoggerFactory.getLogger(RealMachineInfomation.class);
    private Map<String, Long> lastNetworkStatusMap = new HashMap<String, Long>();

    /**
     * memory attributes
     */
    private double memFree;
    private long memTotal;
    private double memUsed;

    /**
     * CPU attributes
     */
    private double cpuIdle;
    // private double cpuSys;
    // private double cpuWait;
    // private double cpuNice;
    // private double cpuCombined;

    /**
     * net attributes
     */
    private long rxPackets;
    private long txPackets;
    private long rxBytes;
    private long txBytes;
    private long rxErrors;
    private long txErrors;
    private long rxDropped;
    private long txDropped;

    @Override
    protected void modifyData() throws Exception {
        Sigar sigar = new Sigar();

        // MEMORY
        memory(sigar);

        // CPU
        cpu(sigar);

        // NET
        network(sigar);
    }

    private void memory(Sigar sigar) throws Exception {
        Mem memory = null;
        try {
            memory = sigar.getMem();
        } catch (SigarException e) {
            logger.error("Caught an exception when get the real machine memory usage.", e);
            throw new Exception();
        }
        this.memTotal = memory.getTotal();
        this.memFree = (double) memory.getFree() / memTotal;
        this.memUsed = (double) memory.getUsed() / memTotal;
    }

    private void cpu(Sigar sigar) throws Exception {
        CpuPerc cpu = null;
        try {
            cpu = sigar.getCpuPerc();
            this.cpuIdle = cpu.getIdle();
        } catch (SigarException e) {
            logger.error("Caught an exception when get the real machine cpu usage.", e);
            throw new Exception();
        }
    }

    private void sendAlarm(Sigar sigar, String netcardName, String computerName) throws Exception {
        AlarmReporter alarmReporter = JmxAgent.getInstance().getAlarmReporter();
        NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(netcardName);
        Long lastNetworkStatus = lastNetworkStatusMap.get(netcardName);
        if (lastNetworkStatus == null) {
            lastNetworkStatusMap.put(netcardName, ifconfig.getFlags());

            if ((ifconfig.getFlags() & NetFlags.IFF_RUNNING) <= 0L || (ifconfig.getFlags() & NetFlags.IFF_UP) <= 0L) {
                AlarmMessageData alarmData = new AlarmMessageData();
                alarmData.setAlarmLevel(AlarmLevel.MAJOR);
                alarmData.setAlarmName("Network Alarm");
                alarmData.setAlarmObject(computerName + ":" + netcardName);
                String description = String.format(
                        "%s is not running well,maybe the net wire or the net card has been pulled out", netcardName);
                alarmData.setAlarmDescription(description);
                alarmData.setOper(AlarmOper.APPEAR);
                alarmReporter.report(alarmData);
                logger.warn("!IFF_UP...skipping getNetInterfaceStat");
                alarmReporter.report(alarmData);
            }
        } else {
            logger.debug("current ifconfig: {}, last status: {}", ifconfig, lastNetworkStatus);
            if (lastNetworkStatus.equals(ifconfig.getFlags())) {
                // last status is equal with current status, do nothing.
            } else {
                AlarmMessageData alarmData = new AlarmMessageData();
                alarmData.setAlarmLevel(AlarmLevel.MAJOR);
                alarmData.setAlarmName("Network Alarm");
                alarmData.setAlarmObject(computerName + ":" + netcardName);
                if ((ifconfig.getFlags() & NetFlags.IFF_RUNNING) <= 0L
                        || (ifconfig.getFlags() & NetFlags.IFF_UP) <= 0L) {
                    String description = String.format(
                            "%s is not running well,maybe the net wire or the net card has been pulled out",
                            netcardName);
                    alarmData.setAlarmDescription(description);
                    alarmData.setOper(AlarmOper.APPEAR);
                    alarmReporter.report(alarmData);
                    logger.warn("!IFF_UP...skipping getNetInterfaceStat");
                } else {
                    alarmData.setOper(AlarmOper.DISAPPEAR);
                }
                alarmReporter.report(alarmData);
            }
        }
        lastNetworkStatusMap.put(netcardName, ifconfig.getFlags());
    }

    private void network(Sigar sigar) throws Exception {
        this.rxPackets = 0;
        this.txPackets = 0;
        this.rxBytes = 0;
        this.txBytes = 0;
        this.rxErrors = 0;
        this.txErrors = 0;
        this.rxDropped = 0;
        this.txDropped = 0;
        try {
            // get computer name
            String computerName = getHostName();

            String ifNames[] = sigar.getNetInterfaceList();
            for (int i = 0; i < ifNames.length; i++) {
                String name = ifNames[i];

                // send alarm
                sendAlarm(sigar, name, computerName);

                NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
                this.rxPackets += ifstat.getRxPackets();
                this.txPackets += ifstat.getTxPackets();
                this.rxBytes += ifstat.getRxBytes();
                this.txBytes += ifstat.getTxBytes();
                this.rxErrors += ifstat.getRxErrors();
                this.txErrors += ifstat.getTxErrors();
                this.rxDropped += ifstat.getRxDropped();
                this.txDropped += ifstat.getTxDropped();
            }
        } catch (Exception e) {
            logger.error("Caught an exception", e);
            throw e;
        }
    }

    @Description(value = "Free memory")
    @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    public double getMemFree() {
        return memFree;
    }

    public void setMemFree(long memFree) {
        this.memFree = memFree;
    }

    @Description(value = "Total memory")
    @ManagedAttribute(unitOfMeasurement = "Byte")
    public long getMemTotal() {
        return memTotal;
    }

    public void setMemTotal(long memTotal) {
        this.memTotal = memTotal;
    }

    @Description(value = "Been used memory")
    @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    public double getMemUsed() {
        return memUsed;
    }

    public void setMemUsed(long memUsed) {
        this.memUsed = memUsed;
    }

    @Description(value = "CPU idle")
    @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    public double getCpuIdle() {
        return cpuIdle;
    }

    public void setCpuIdle(double cpuIdle) {
        this.cpuIdle = cpuIdle;
    }

    // --------------------------------------------
    // @Description(value = "CPU idle")
    // @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    // public double getCpuSys() {
    // return cpuSys;
    // }
    //
    // public void setCpuSys(double cpuSys) {
    // this.cpuSys = cpuSys;
    // }
    //
    // @Description(value = "CPU idle")
    // @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    // public double getCpuWait() {
    // return cpuWait;
    // }
    //
    // public void setCpuWait(double cpuWait) {
    // this.cpuWait = cpuWait;
    // }
    //
    // @Description(value = "CPU nice")
    // @ManagedAttribute(range = "[0,100]", unitOfMeasurement = "percentage(%)")
    // public double getCpuNice() {
    // return cpuNice;
    // }
    //
    // public void setCpuNice(double cpuNice) {
    // this.cpuNice = cpuNice;
    // }
    //
    // @Description(value = "CPU idle")
    // @ManagedAttribute(range = "[0,1]", unitOfMeasurement = "percentage(%)")
    // public double getCpuCombined() {
    // return cpuCombined;
    // }
    //
    // public void setCpuCombined(double cpuCombined) {
    // this.cpuCombined = cpuCombined;
    // }

    @Description(value = "number of network packets been received")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getRxPackets() {
        return rxPackets;
    }

    public void setRxPackets(long rxPackets) {
        this.rxPackets = rxPackets;
    }

    @Description(value = "number of network packets been sent ")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getTxPackets() {
        return txPackets;
    }

    public void setTxPackets(long txPackets) {
        this.txPackets = txPackets;
    }

    @Description(value = "Bytes of network packets been received")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getRxBytes() {
        return rxBytes;
    }

    public void setRxBytes(long rxBytes) {
        this.rxBytes = rxBytes;
    }

    @Description(value = "Bytes of network packets been sent ")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getTxBytes() {
        return txBytes;
    }

    public void setTxBytes(long txBytes) {
        this.txBytes = txBytes;
    }

    @Description(value = "number of network receiving errors")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getRxErrors() {
        return rxErrors;
    }

    public void setRxErrors(long rxErrors) {
        this.rxErrors = rxErrors;
    }

    @Description(value = "number of network sending errors")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getTxErrors() {
        return txErrors;
    }

    public void setTxErrors(long txErrors) {
        this.txErrors = txErrors;
    }

    @Description(value = "number of network receiving dropped")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getRxDropped() {
        return rxDropped;
    }

    public void setRxDropped(long rxDropped) {
        this.rxDropped = rxDropped;
    }

    @Description(value = "number of network sending dropped")
    @ManagedAttribute(unitOfMeasurement = "number")
    public long getTxDropped() {
        return txDropped;
    }

    public void setTxDropped(long txDropped) {
        this.txDropped = txDropped;
    }

    private String getHostName() throws UnknownHostException {
        // try InetAddress.LocalHost first;
        // NOTE: InetAddress.getLocalHost().getHostName() will not work in certain environments.
        try {
            String result = InetAddress.getLocalHost().getHostName();
            if (result == null)
                return "UNKNOWN HOST";
            return result;
        } catch (UnknownHostException e) {
            // try environment properties.
            String host = System.getenv("COMPUTERNAME");
            if (host != null)
                return host;
            host = System.getenv("HOSTNAME");
            if (host != null)
                return host;

            throw e;
        }
    }

    @Override
    public String toString() {
        return "RealMachineInfomation [memFree=" + memFree + ", memTotal=" + memTotal + ", memUsed=" + memUsed
                + ", cpuIdle=" + cpuIdle + ", rxPackets=" + rxPackets + ", txPackets=" + txPackets + ", rxBytes="
                + rxBytes + ", txBytes=" + txBytes + ", rxErrors=" + rxErrors + ", txErrors=" + txErrors
                + ", rxDropped=" + rxDropped + ", txDropped=" + txDropped + "]";
    }

}
