package py.monitor.jmx.mbeans;

import java.rmi.server.RemoteServer;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.customizable.repository.AttributeMetadata;
import py.monitor.jmx.server.JmxAgent;
import py.monitor.jmx.server.ResourceRelatedAttribute;
import py.monitor.task.TimeSpan;

/**
 * This class is used to create monitor MBeans.
 * 
 * @author sxl
 * 
 */
public class JmxRmiAgent extends RemoteServer implements JmxRmiAgentMBean {
    private static final long serialVersionUID = 5388357648827099514L;
    private static final Logger logger = LoggerFactory.getLogger(JmxRmiAgent.class);
    private JmxAgent agent;

    @Override
    public String createSubTask(long parentTaskId, long period, List<TimeSpan> runTime,
            List<ResourceRelatedAttribute> resourceReleatedAttribute) throws Exception {
        if (agent == null) {
            throw new Exception();
        }
        logger.debug("Going to create sub-task of performance task from monitor-center");

        return agent.addPerformanceReporter(parentTaskId,period, runTime, resourceReleatedAttribute);
    }

    @Override
    public void destroySubTask(String objectName) throws Exception {
        if (agent == null) {
            throw new Exception();
        }
        logger.debug("Going to destroy sub-task of performance task from monitor-center");
        agent.removePerformanceReporter(objectName);
    }

    @Override
    public Set<AttributeMetadata> getMbeans(Set<String> domains) throws Exception {
        if (agent == null) {
            throw new Exception();
        }
        logger.debug("Going to get all mbeans from domain : {}", domains);
        return agent.getMbeans(domains);
    }

    public JmxAgent getAgent() {
        return agent;
    }

    public void setAgent(JmxAgent agent) {
        this.agent = agent;
    }

}
